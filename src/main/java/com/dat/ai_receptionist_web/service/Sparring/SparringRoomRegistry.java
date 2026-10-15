package com.dat.ai_receptionist_web.service.Sparring;

import com.dat.ai_receptionist_web.dto.Sparring.SparringRoomResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SparringErrorCode;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Service
public class SparringRoomRegistry {
    private static final int MAX_COMPETITOR_NAME_LENGTH = 30;
    private static final int MAX_SCORE_DELTA = 99;
    private static final int MAX_PENALTY_DELTA = 20;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final CopyOnWriteArraySet<Consumer<SparringRoomResponse>> snapshotListeners = new CopyOnWriteArraySet<>();
    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();

    public SparringRoomRegistry() {
        ticker.scheduleAtFixedRate(this::tickAll, 1, 1, TimeUnit.SECONDS);
    }

    public SparringRoomResponse create() {
        for (int attempts = 0; attempts < 100; attempts++) {
            String roomCode = String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
            Room room = new Room(roomCode);
            if (rooms.putIfAbsent(roomCode, room) == null) return response(room.snapshot());
        }
        throw new IllegalStateException("Unable to allocate sparring room code");
    }

    public SparringRoomResponse get(String roomCode) {
        return response(room(roomCode).snapshot());
    }

    public SparringRoomResponse apply(String roomCode, boolean operator, String type, String side, Integer points,
                                      Integer delta, Integer seconds, Integer round, String name) {
        return response(room(roomCode).apply(operator, type, side, points, delta, seconds, round, name));
    }

    public boolean exists(String roomCode) { return rooms.containsKey(roomCode); }
    public AutoCloseable onSnapshot(Consumer<SparringRoomResponse> listener) {
        snapshotListeners.add(listener);
        return () -> snapshotListeners.remove(listener);
    }

    private Room room(String roomCode) {
        if (roomCode == null || !roomCode.matches("\\d{4}")) throw new ApiException(SparringErrorCode.INVALID_ROOM_CODE);
        Room room = rooms.get(roomCode);
        if (room == null) throw new ApiException(SparringErrorCode.ROOM_NOT_FOUND);
        return room;
    }

    private void tickAll() {
        for (Room room : rooms.values()) {
            Snapshot before = room.snapshot();
            Snapshot after = room.tick();
            if (!before.equals(after)) {
                SparringRoomResponse response = response(after);
                for (Consumer<SparringRoomResponse> listener : snapshotListeners) listener.accept(response);
            }
        }
    }

    private SparringRoomResponse response(Snapshot snapshot) {
        return new SparringRoomResponse(snapshot.roomCode(), new SparringRoomResponse.Snapshot(
                snapshot.roomCode(), snapshot.redName(), snapshot.blueName(), snapshot.round(),
                snapshot.remainingSeconds(), snapshot.redScore(), snapshot.blueScore(),
                snapshot.redPenalties(), snapshot.bluePenalties(), snapshot.running(), snapshot.ended()));
    }

    @PreDestroy void shutdown() { ticker.shutdownNow(); }

    private static final class Room {
        private Snapshot state;
        private Room(String roomCode) {
            state = new Snapshot(roomCode, "RED", "BLUE", 1, 120, 0, 0, 0, 0, false, false);
        }
        synchronized Snapshot snapshot() { return state; }
        synchronized Snapshot tick() {
            if (!state.running() || state.ended()) return state;
            int remaining = Math.max(0, state.remainingSeconds() - 1);
            state = state.withRemainingSeconds(remaining).withRunning(remaining > 0);
            return state;
        }
        synchronized Snapshot apply(boolean operator, String type, String side, Integer points, Integer delta,
                                    Integer seconds, Integer round, String name) {
            if ("judgeScore".equals(type)) {
                if (points == null || points < 1 || points > 5) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
                return changeScore(side, points);
            }
            if (!operator) throw new ApiException(SparringErrorCode.COMMAND_NOT_ALLOWED);
            return switch (type) {
                case "operatorAdjustScore" -> changeScore(side, requireDelta(delta, MAX_SCORE_DELTA));
                case "operatorAdjustPenalty" -> changePenalty(side, requireDelta(delta, MAX_PENALTY_DELTA));
                case "setCompetitorName" -> setCompetitorName(side, name);
                case "setRound" -> setRound(round);
                case "setDuration" -> setDuration(seconds);
                case "start" -> state = state.withRunning(!state.ended() && state.remainingSeconds() > 0);
                case "pause" -> state = state.withRunning(false);
                case "end" -> state = state.withRunning(false).withEnded(true);
                default -> throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            };
        }
        private int requireDelta(Integer value, int maximumMagnitude) {
            if (value == null || value == 0 || Math.abs((long) value) > maximumMagnitude) {
                throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            }
            return value;
        }
        private Snapshot setCompetitorName(String side, String value) {
            if (value == null) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            String normalized = value.strip();
            if (normalized.isEmpty() || normalized.length() > MAX_COMPETITOR_NAME_LENGTH) {
                throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            }
            if ("red".equals(side)) return state = state.withRedName(normalized);
            if ("blue".equals(side)) return state = state.withBlueName(normalized);
            throw new ApiException(SparringErrorCode.COMMAND_INVALID);
        }
        private Snapshot changeScore(String side, int amount) {
            if (state.ended()) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            if ("red".equals(side)) return state = state.withRedScore(Math.max(0, state.redScore() + amount));
            if ("blue".equals(side)) return state = state.withBlueScore(Math.max(0, state.blueScore() + amount));
            throw new ApiException(SparringErrorCode.COMMAND_INVALID);
        }
        private Snapshot changePenalty(String side, int amount) {
            if ("red".equals(side)) return state = state.withRedPenalties(Math.max(0, state.redPenalties() + amount));
            if ("blue".equals(side)) return state = state.withBluePenalties(Math.max(0, state.bluePenalties() + amount));
            throw new ApiException(SparringErrorCode.COMMAND_INVALID);
        }
        private Snapshot setRound(Integer value) {
            if (value == null || value < 1 || value > 3) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            return state = state.withRound(value);
        }
        private Snapshot setDuration(Integer value) {
            if (value == null || value < 0 || value > 599) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            return state = state.withRemainingSeconds(value).withRunning(false);
        }
    }

    private record Snapshot(String roomCode, String redName, String blueName, int round, int remainingSeconds,
                            int redScore, int blueScore,
                            int redPenalties, int bluePenalties, boolean running, boolean ended) {
        Snapshot withRedName(String value) { return new Snapshot(roomCode, value, blueName, round, remainingSeconds, redScore, blueScore, redPenalties, bluePenalties, running, ended); }
        Snapshot withBlueName(String value) { return new Snapshot(roomCode, redName, value, round, remainingSeconds, redScore, blueScore, redPenalties, bluePenalties, running, ended); }
        Snapshot withRound(int value) { return new Snapshot(roomCode, redName, blueName, value, remainingSeconds, redScore, blueScore, redPenalties, bluePenalties, running, ended); }
        Snapshot withRemainingSeconds(int value) { return new Snapshot(roomCode, redName, blueName, round, value, redScore, blueScore, redPenalties, bluePenalties, running, ended); }
        Snapshot withRedScore(int value) { return new Snapshot(roomCode, redName, blueName, round, remainingSeconds, value, blueScore, redPenalties, bluePenalties, running, ended); }
        Snapshot withBlueScore(int value) { return new Snapshot(roomCode, redName, blueName, round, remainingSeconds, redScore, value, redPenalties, bluePenalties, running, ended); }
        Snapshot withRedPenalties(int value) { return new Snapshot(roomCode, redName, blueName, round, remainingSeconds, redScore, blueScore, value, bluePenalties, running, ended); }
        Snapshot withBluePenalties(int value) { return new Snapshot(roomCode, redName, blueName, round, remainingSeconds, redScore, blueScore, redPenalties, value, running, ended); }
        Snapshot withRunning(boolean value) { return new Snapshot(roomCode, redName, blueName, round, remainingSeconds, redScore, blueScore, redPenalties, bluePenalties, value, ended); }
        Snapshot withEnded(boolean value) { return new Snapshot(roomCode, redName, blueName, round, remainingSeconds, redScore, blueScore, redPenalties, bluePenalties, running, value); }
    }
}
