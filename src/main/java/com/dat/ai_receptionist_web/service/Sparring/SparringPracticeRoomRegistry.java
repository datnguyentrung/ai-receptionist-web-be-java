package com.dat.ai_receptionist_web.service.Sparring;

import com.dat.ai_receptionist_web.dto.Sparring.SparringPracticeCreateRequest;
import com.dat.ai_receptionist_web.dto.Sparring.SparringPracticeRoomResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SparringErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Owner của state buổi tập trọng tài. Room này độc lập với SparringRoomRegistry để
 * điểm luyện tập, video, presence và quy tắc đồng thuận không làm đổi Đối kháng thật.
 */
@Service
public class SparringPracticeRoomRegistry {
    private static final long HEARTBEAT_TIMEOUT_MS = 15_000;
    private static final long COUNTDOWN_MS = 3_000;
    private static final long SCORING_WINDOW_MS = 2_000;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final Set<Consumer<SparringPracticeRoomResponse>> listeners = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();

    public SparringPracticeRoomRegistry() {
        ticker.scheduleAtFixedRate(this::tickAll, 200, 200, TimeUnit.MILLISECONDS);
    }

    public SparringPracticeRoomResponse create(SparringPracticeCreateRequest request) {
        if (request.consensusJudges() > request.maximumJudges()) throw invalid();
        for (int attempt = 0; attempt < 100; attempt++) {
            String code = String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
            Room room = new Room(code, request.maximumJudges(), request.consensusJudges());
            if (rooms.putIfAbsent(code, room) == null) return response(room.snapshot());
        }
        throw new IllegalStateException("Unable to allocate practice room code");
    }

    public boolean exists(String roomCode) { return rooms.containsKey(roomCode); }
    public SparringPracticeRoomResponse get(String roomCode) { return response(room(roomCode).snapshot()); }
    public AutoCloseable onSnapshot(Consumer<SparringPracticeRoomResponse> listener) { listeners.add(listener); return () -> listeners.remove(listener); }

    public SparringPracticeRoomResponse connected(String roomCode, UUID userId, boolean operator) {
        Room room = room(roomCode); Snapshot snapshot = room.connected(userId, operator); publish(snapshot); return response(snapshot);
    }
    public void disconnected(String roomCode, UUID userId, boolean operator) {
        if (operator || !exists(roomCode)) return; Snapshot snapshot = room(roomCode).disconnected(userId); publish(snapshot);
    }
    public SparringPracticeRoomResponse command(String roomCode, UUID userId, boolean operator, String type, String side, Integer points) {
        Snapshot snapshot = room(roomCode).command(userId, operator, type, side, points); publish(snapshot); return response(snapshot);
    }
    public SparringPracticeRoomResponse videoUploaded(String roomCode, String objectPath, String signedUrl) {
        Snapshot snapshot = room(roomCode).videoUploaded(objectPath, signedUrl); publish(snapshot); return response(snapshot);
    }
    /** Đóng buổi tập và trả path để controller xóa object private khỏi storage. */
    public String close(String roomCode) { return room(roomCode).close(); }
    public String expectedVideoPath(String roomCode) { return "practice/" + room(roomCode).code + "/video.mp4"; }
    /** Scheduler gọi hàm này để dọn buổi bị bỏ quên quá 24 giờ và giải phóng memory. */
    public List<String> removeAbandonedVideoPaths(long olderThanEpochMs) {
        List<String> paths = new ArrayList<>();
        rooms.forEach((code, room) -> { if (room.lastTouched() < olderThanEpochMs && rooms.remove(code, room)) { String path=room.objectPath(); if(path!=null) paths.add(path); } });
        return paths;
    }

    private Room room(String code) {
        if (code == null || !code.matches("\\d{4}")) throw new ApiException(SparringErrorCode.INVALID_ROOM_CODE);
        Room room = rooms.get(code); if (room == null) throw new ApiException(SparringErrorCode.ROOM_NOT_FOUND); return room;
    }
    private void tickAll() { for (Room room : rooms.values()) { Snapshot changed = room.tick(); if (changed != null) publish(changed); } }
    private void publish(Snapshot snapshot) { SparringPracticeRoomResponse value = response(snapshot); listeners.forEach(listener -> listener.accept(value)); }
    private static SparringPracticeRoomResponse response(Snapshot snapshot) { return new SparringPracticeRoomResponse(snapshot.code, snapshot.toDto()); }
    private static ApiException invalid() { return new ApiException(SparringErrorCode.COMMAND_INVALID); }

    private static final class Room {
        private final String code; private final int maximumJudges; private final int consensusJudges;
        private final LinkedHashMap<UUID, JudgeState> judges = new LinkedHashMap<>();
        private Snapshot state;
        private long lastTouched = Instant.now().toEpochMilli();
        private Room(String code, int maximumJudges, int consensusJudges) { this.code=code; this.maximumJudges=maximumJudges; this.consensusJudges=consensusJudges; state=new Snapshot(code, maximumJudges, consensusJudges); }
        synchronized Snapshot snapshot() { return state.copy(judges); }
        synchronized Snapshot connected(UUID userId, boolean operator) {
            lastTouched=Instant.now().toEpochMilli();
            if (!operator) {
                JudgeState judge = judges.get(userId);
                if (judge == null) { if (judges.size() >= maximumJudges) throw invalid(); judge = new JudgeState(judges.size()+1); judges.put(userId, judge); }
                judge.connected=true; judge.ready=false; judge.lastSeen=Instant.now().toEpochMilli();
            }
            return state.copy(judges);
        }
        synchronized Snapshot disconnected(UUID userId) {
            lastTouched=Instant.now().toEpochMilli();
            JudgeState judge=judges.get(userId); if (judge == null) return state.copy(judges);
            judge.connected=false; judge.ready=false; pauseForDisconnect(); return state.copy(judges);
        }
        synchronized Snapshot command(UUID userId, boolean operator, String type, String side, Integer points) {
            lastTouched=Instant.now().toEpochMilli();
            if ("heartbeat".equals(type)) { touch(userId, operator); return state.copy(judges); }
            if ("ready".equals(type)) { if (operator || !state.videoUploaded) throw invalid(); JudgeState judge=requireJudge(userId); judge.ready=true; judge.lastSeen=Instant.now().toEpochMilli(); return state.copy(judges); }
            if ("notReady".equals(type)) { if (operator) throw invalid(); requireJudge(userId).ready=false; return state.copy(judges); }
            if ("judgeScore".equals(type)) return vote(userId, operator, side, points);
            if (!operator) throw new ApiException(SparringErrorCode.COMMAND_NOT_ALLOWED);
            if ("start".equals(type) || "resume".equals(type)) { if (!canStart()) throw invalid(); state.status="COUNTDOWN"; state.countdownEndsAt=Instant.now().toEpochMilli()+COUNTDOWN_MS; return state.copy(judges); }
            if ("reset".equals(type)) { state.reset(); judges.values().forEach(j -> j.ready=false); return state.copy(judges); }
            if ("close".equals(type)) { state.status="CLOSED"; return state.copy(judges); }
            if ("videoEnded".equals(type)) { state.status="FINISHED"; return state.copy(judges); }
            throw invalid();
        }
        synchronized Snapshot videoUploaded(String objectPath, String signedUrl) { lastTouched=Instant.now().toEpochMilli(); if (state.videoUploaded) throw invalid(); state.videoUploaded=true; state.objectPath=objectPath; state.videoUrl=signedUrl; return state.copy(judges); }
        synchronized String close() { state.status="CLOSED"; return state.objectPath; }
        synchronized long lastTouched() { return lastTouched; }
        synchronized String objectPath() { return state.objectPath; }
        synchronized Snapshot tick() {
            long now=Instant.now().toEpochMilli(); boolean changed=false;
            for (JudgeState judge:judges.values()) if (judge.connected && now-judge.lastSeen>HEARTBEAT_TIMEOUT_MS) { judge.connected=false; judge.ready=false; pauseForDisconnect(); changed=true; }
            if ("COUNTDOWN".equals(state.status) && now>=state.countdownEndsAt) { state.status="RUNNING"; state.startedAt=now; state.countdownEndsAt=0; changed=true; }
            if (state.voteWindowEndsAt>0 && now>=state.voteWindowEndsAt) { state.voteWindowEndsAt=0; state.votes.clear(); state.resolved=false; changed=true; }
            return changed ? state.copy(judges) : null;
        }
        private Snapshot vote(UUID userId, boolean operator, String side, Integer points) {
            if (operator || !"RUNNING".equals(state.status) || !("red".equals(side)||"blue".equals(side)) || points==null || points<1 || points>5) throw invalid();
            requireJudge(userId); long now=Instant.now().toEpochMilli();
            if (state.voteWindowEndsAt==0 || now>=state.voteWindowEndsAt) { state.votes.clear(); state.resolved=false; state.voteWindowEndsAt=now+SCORING_WINDOW_MS; }
            if (state.votes.containsKey(userId)) throw invalid();
            state.votes.put(userId, side+":"+points);
            long count=state.votes.values().stream().filter((side+":"+points)::equals).count();
            if (count==consensusJudges && !state.resolved) { if ("red".equals(side)) state.redScore+=points; else state.blueScore+=points; state.resolved=true; }
            return state.copy(judges);
        }
        private void touch(UUID userId, boolean operator) { if (!operator) { JudgeState j=requireJudge(userId); j.lastSeen=Instant.now().toEpochMilli(); } }
        private JudgeState requireJudge(UUID id) { JudgeState judge=judges.get(id); if (judge==null || !judge.connected) throw invalid(); return judge; }
        private boolean canStart() { return state.videoUploaded && judges.size()==maximumJudges && judges.values().stream().allMatch(j -> j.connected && j.ready); }
        private void pauseForDisconnect() { if ("RUNNING".equals(state.status)) { state.mediaOffset+=Math.max(0, Instant.now().toEpochMilli()-state.startedAt); state.startedAt=0; state.status="PAUSED"; } else if ("COUNTDOWN".equals(state.status)) state.status="PAUSED"; }
    }
    private static final class JudgeState { final int position; boolean connected; boolean ready; long lastSeen; JudgeState(int position){this.position=position;} }
    private static final class Snapshot {
        final String code; final int maximumJudges; final int consensusJudges; int redScore; int blueScore; String status="WAITING"; long countdownEndsAt; long mediaOffset; long startedAt; boolean videoUploaded; String objectPath; String videoUrl; long voteWindowEndsAt; boolean resolved; final Map<UUID,String> votes=new HashMap<>();
        Snapshot(String code,int maximumJudges,int consensusJudges){this.code=code;this.maximumJudges=maximumJudges;this.consensusJudges=consensusJudges;}
        Snapshot copy(Map<UUID,JudgeState> judges){ Snapshot copy=new Snapshot(code,maximumJudges,consensusJudges); copy.redScore=redScore;copy.blueScore=blueScore;copy.status=status;copy.countdownEndsAt=countdownEndsAt;copy.mediaOffset=mediaOffset;copy.startedAt=startedAt;copy.videoUploaded=videoUploaded;copy.objectPath=objectPath;copy.videoUrl=videoUrl;copy.voteWindowEndsAt=voteWindowEndsAt;copy.resolved=resolved;copy.votes.putAll(votes);copy.judges=judges;return copy; }
        private Map<UUID,JudgeState> judges=Map.of();
        SparringPracticeRoomResponse.Snapshot toDto(){ long offset=mediaOffset+("RUNNING".equals(status)?Math.max(0,Instant.now().toEpochMilli()-startedAt):0); List<SparringPracticeRoomResponse.Judge> list=judges.values().stream().map(j->new SparringPracticeRoomResponse.Judge(j.position,j.connected,j.ready)).toList(); boolean ready=videoUploaded&&judges.size()==maximumJudges&&judges.values().stream().allMatch(j->j.connected&&j.ready); return new SparringPracticeRoomResponse.Snapshot(code,maximumJudges,consensusJudges,2,redScore,blueScore,status,countdownEndsAt,offset,videoUrl,videoUploaded,ready,list); }
        void reset(){redScore=0;blueScore=0;status="WAITING";countdownEndsAt=0;mediaOffset=0;startedAt=0;voteWindowEndsAt=0;resolved=false;votes.clear();}
    }
}
