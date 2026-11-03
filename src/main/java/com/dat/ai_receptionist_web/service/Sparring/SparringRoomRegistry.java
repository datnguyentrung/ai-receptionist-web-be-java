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
/**
 * Module quản lý vòng đời và trạng thái của toàn bộ phòng Đối kháng trong memory.
 *
 * <p>Registry chịu trách nhiệm sinh mã phòng, tìm phòng, chuyển command vào đúng
 * phòng và phát snapshot mới cho các listener WebSocket. Dữ liệu hiện tại không
 * được lưu xuống database nên sẽ mất khi backend khởi động lại.</p>
 */
public class SparringRoomRegistry {
    // Giới hạn độ dài tên võ sĩ để tránh dữ liệu quá dài đi vào state và giao diện.
    private static final int MAX_COMPETITOR_NAME_LENGTH = 30;
    // Độ lớn tuyệt đối tối đa cho một lần operator điều chỉnh điểm.
    private static final int MAX_SCORE_DELTA = 99;
    // Độ lớn tuyệt đối tối đa cho một lần operator điều chỉnh penalty.
    private static final int MAX_PENALTY_DELTA = 20;

    // Map lưu các room theo mã 4 chữ số; ConcurrentHashMap hỗ trợ truy cập từ nhiều thread.
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    // Danh sách callback nhận snapshot khi timer làm thay đổi trạng thái room.
    private final CopyOnWriteArraySet<Consumer<SparringRoomResponse>> snapshotListeners = new CopyOnWriteArraySet<>();
    // Scheduler nền dùng để giảm đồng hồ của các trận đang chạy mỗi giây.
    private final ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor();

    /**
     * Khởi tạo registry và bắt đầu bộ đếm thời gian dùng chung cho các room.
     *
     * <p>Tham chiếu method {@code this::tickAll} được scheduler gọi sau 1 giây
     * và lặp lại sau mỗi 1 giây.</p>
     */
    public SparringRoomRegistry() {
        // Mỗi giây kiểm tra các room đang chạy để giảm thời gian còn lại của trận.
        ticker.scheduleAtFixedRate(
                this::tickAll,
                1,
                1,
                TimeUnit.SECONDS
        );
    }

    /**
     * Tạo một room mới với mã ngẫu nhiên gồm đúng 4 chữ số.
     *
     * @return DTO chứa mã room và snapshot mặc định của room vừa tạo
     * @throws IllegalStateException nếu sau 100 lần thử vẫn không tìm được mã chưa dùng
     */
    public SparringRoomResponse create() {
        // Thử nhiều lần vì mã ngẫu nhiên có thể trùng với một room đang tồn tại.
        for (int attempts = 0; attempts < 100; attempts++) {
            // Sinh số từ 0 đến 9999, sau đó định dạng thành chuỗi 4 chữ số, ví dụ 7 thành "0007".
            String roomCode = String.format(
                    "%04d",
                    ThreadLocalRandom.current().nextInt(10_000)
            );

            // Tạo state mặc định cho room trước khi đưa room vào registry.
            Room room = new Room(roomCode);

            // putIfAbsent chỉ thêm khi mã chưa tồn tại; null nghĩa là thêm thành công.
            // Cơ chế này tránh ghi đè room cũ nếu hai request sinh cùng một mã.
            if (rooms.putIfAbsent(
                    roomCode,
                    room
            ) == null) return response(room.snapshot());
        }

        // Khả năng xảy ra rất thấp; báo lỗi thay vì trả về room không hợp lệ.
        throw new IllegalStateException("Unable to allocate sparring room code");
    }

    /**
     * Lấy snapshot hiện tại của một room.
     *
     * @param roomCode mã room 4 chữ số cần tra cứu
     * @return DTO snapshot để trả về cho REST hoặc WebSocket
     * @throws ApiException nếu mã sai định dạng hoặc room không tồn tại
     */
    public SparringRoomResponse get(String roomCode) {
        // room(...) vừa validate vừa lookup, giúp mọi caller dùng chung quy tắc lỗi.
        return response(room(roomCode).snapshot());
    }

    /**
     * Áp dụng một command vào room và trả về state sau khi xử lý.
     *
     * @param roomCode mã room nhận command
     * @param operator true nếu client có quyền điều khiển operator
     * @param type loại command, ví dụ judgeScore, start, pause hoặc end
     * @param side bên thi đấu cần tác động, thường là red hoặc blue
     * @param points số điểm cho command judgeScore
     * @param delta mức tăng/giảm điểm hoặc penalty của operator
     * @param seconds thời lượng mới khi dùng command setDuration
     * @param round hiệp mới khi dùng command setRound
     * @param name tên võ sĩ mới khi dùng command setCompetitorName
     * @return DTO snapshot sau khi command thành công
     * @throws ApiException nếu room hoặc command không hợp lệ, hoặc caller thiếu quyền
     */
    public SparringRoomResponse apply(
            String roomCode,
            boolean operator,
            String type,
            String side,
            Integer points,
            Integer delta,
            Integer seconds,
            Integer round,
            String name
    ) {
        // Chuyển command dạng các field rời từ WebSocket thành lời gọi xử lý trên đúng room.
        return response(room(roomCode).apply(
                operator,
                type,
                side,
                points,
                delta,
                seconds,
                round,
                name
        ));
    }

    /**
     * Kiểm tra nhanh một room có đang được registry lưu hay không.
     *
     * @param roomCode mã room cần kiểm tra
     * @return true nếu map có room tương ứng, ngược lại false
     */
    public boolean exists(String roomCode) {
        // Dùng trong handshake WebSocket để tránh kết nối vào room không còn tồn tại.
        return rooms.containsKey(roomCode);
    }

    /**
     * Đăng ký listener nhận snapshot khi timer làm thay đổi state của room.
     *
     * @param listener callback được gọi với DTO snapshot mới
     * @return handle hủy đăng ký; caller nên gọi khi listener không còn cần thiết
     */
    public AutoCloseable onSnapshot(Consumer<SparringRoomResponse> listener) {
        // CopyOnWriteArraySet cho phép đăng ký/hủy listener an toàn khi đang broadcast.
        snapshotListeners.add(listener);
        // Trả về lambda đóng vai trò như một unsubscribe handle.
        return () -> snapshotListeners.remove(listener);
    }

    /**
     * Validate mã room rồi lấy object Room tương ứng.
     *
     * @param roomCode mã cần kiểm tra và tra cứu
     * @return object Room nội bộ tương ứng với mã
     * @throws ApiException nếu mã không phải 4 chữ số hoặc không tồn tại
     */
    private Room room(String roomCode) {
        // Tập trung validation và lookup để mọi thao tác room dùng cùng một quy tắc lỗi.
        if (roomCode == null || !roomCode.matches("\\d{4}"))
            throw new ApiException(SparringErrorCode.INVALID_ROOM_CODE);

        // ConcurrentHashMap trả null nếu không có room với mã này.
        Room room = rooms.get(roomCode);
        if (room == null) throw new ApiException(SparringErrorCode.ROOM_NOT_FOUND);
        return room;
    }

    /**
     * Tick toàn bộ room một lần.
     *
     * <p>Hàm được scheduler gọi mỗi giây. Mỗi room tự quyết định có cần giảm
     * thời gian hay không; chỉ state thay đổi mới được broadcast.</p>
     */
    private void tickAll() {
        // Duyệt qua snapshot hiện tại của map; room có thể được thêm trong lúc app đang chạy.
        for (Room room : rooms.values()) {
            // Lưu state trước tick để biết timer có thực sự thay đổi hay không.
            Snapshot before = room.snapshot();

            // Room.tick() tự bỏ qua room đang pause hoặc đã kết thúc.
            Snapshot after = room.tick();

            // Chỉ phát snapshot khi state thực sự thay đổi, tránh broadcast dư thừa.
            if (!before.equals(after)) {
                SparringRoomResponse response = response(after);
                // Gửi snapshot đến mọi adapter đang quan tâm, hiện tại là WebSocket handler.
                for (Consumer<SparringRoomResponse> listener : snapshotListeners) listener.accept(response);
            }
        }
    }

    /**
     * Chuyển state nội bộ thành DTO công khai của module.
     *
     * @param snapshot state bất biến của room
     * @return response DTO dùng cho REST/WebSocket
     */
    private SparringRoomResponse response(Snapshot snapshot) {
        // Không trả trực tiếp implementation Room/Snapshot nội bộ để giữ seam DTO rõ ràng.
        return new SparringRoomResponse(
                snapshot.roomCode(),
                new SparringRoomResponse.Snapshot(
                        snapshot.roomCode(),
                        snapshot.redName(),
                        snapshot.blueName(),
                        snapshot.round(),
                        snapshot.remainingSeconds(),
                        snapshot.redScore(),
                        snapshot.blueScore(),
                        snapshot.redPenalties(),
                        snapshot.bluePenalties(),
                        snapshot.running(),
                        snapshot.ended()
                )
        );
    }

    /** Dừng scheduler khi Spring shutdown để không còn thread nền chạy ngoài vòng đời app. */
    @PreDestroy
    void shutdown() {
        ticker.shutdownNow();
    }

    /**
     * State và business rule của một room đơn lẻ.
     *
     * <p>Class này là implementation nội bộ của registry, không phải DTO.
     * Mọi thao tác đọc/ghi state đều được đồng bộ hóa để tránh hai command
     * hoặc timer cập nhật cùng lúc làm mất dữ liệu.</p>
     */
    private static final class Room {
        // Snapshot hiện tại; mỗi lần thay đổi sẽ tạo record Snapshot mới.
        private Snapshot state;

        /**
         * Khởi tạo state mặc định cho một phòng mới.
         *
         * @param roomCode mã 4 chữ số đã được registry cấp
         */
        private Room(String roomCode) {
            state = new Snapshot(
                    roomCode,
                    "RED",
                    "BLUE",
                    1,
                    120,
                    0,
                    0,
                    0,
                    0,
                    false,
                    false
            );
        }

        /**
         * Đọc state hiện tại một cách thread-safe.
         *
         * @return snapshot hiện tại của room
         */
        synchronized Snapshot snapshot() {
            // synchronized giúp đọc snapshot nhất quán với các thao tác cập nhật state.
            return state;
        }

        /**
         * Giảm đồng hồ của room một giây nếu trận đang chạy.
         *
         * @return state trước đó nếu room đang pause/ended, hoặc state mới sau khi giảm timer
         */
        synchronized Snapshot tick() {
            // Không giảm timer khi trận chưa start hoặc đã end.
            if (!state.running() || state.ended()) return state;

            // Math.max bảo đảm timer không bao giờ xuống số âm.
            int remaining = Math.max(
                    0,
                    state.remainingSeconds() - 1
            );

            // Khi remaining = 0, running chuyển false để tự dừng trận.
            state = state.withRemainingSeconds(remaining).withRunning(remaining > 0);
            return state;
        }

        /**
         * Xử lý command gửi từ một client trong room.
         *
         * @param operator caller có quyền operator hay không
         * @param type tên command cần xử lý
         * @param side bên đỏ/xanh cần cập nhật
         * @param points số điểm judge muốn cộng
         * @param delta mức thay đổi điểm hoặc penalty của operator
         * @param seconds thời lượng mới của timer
         * @param round số hiệp mới
         * @param name tên võ sĩ mới
         * @return snapshot sau khi áp dụng command
         * @throws ApiException nếu command không hợp lệ hoặc không được phép
         */
        synchronized Snapshot apply(
                boolean operator,
                String type,
                String side,
                Integer points,
                Integer delta,
                Integer seconds,
                Integer round,
                String name
        ) {
            // judgeScore là command duy nhất judge thường được phép gửi.
            if ("judgeScore".equals(type)) {
                // Điểm judge hợp lệ nằm trong khoảng 1 đến 5.
                if (points == null || points < 1 || points > 5)
                    throw new ApiException(SparringErrorCode.COMMAND_INVALID);
                return changeScore(
                        side,
                        points
                );
            }

            // Các command còn lại thay đổi cấu hình/state nên bắt buộc phải là operator.
            if (!operator) throw new ApiException(SparringErrorCode.COMMAND_NOT_ALLOWED);

            // Switch expression chọn đúng business rule tương ứng với type command.
            return switch (type) {
                case "operatorAdjustScore" -> changeScore(
                        side,
                        requireDelta(
                                delta,
                                MAX_SCORE_DELTA
                        )
                );
                case "operatorAdjustPenalty" -> changePenalty(
                        side,
                        requireDelta(
                                delta,
                                MAX_PENALTY_DELTA
                        )
                );
                case "setCompetitorName" -> setCompetitorName(
                        side,
                        name
                );
                case "setRound" -> setRound(round);
                case "setDuration" -> setDuration(seconds);
                // Chỉ start khi room chưa kết thúc và vẫn còn thời gian.
                case "start" -> state = state.withRunning(!state.ended() && state.remainingSeconds() > 0);
                // Pause chỉ dừng timer, không xóa điểm hoặc thời gian còn lại.
                case "pause" -> state = state.withRunning(false);
                // End dừng timer vĩnh viễn theo state hiện tại của room.
                case "end" -> state = state.withRunning(false).withEnded(true);
                // Type không nằm trong protocol được xem là command lỗi.
                default -> throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            };
        }

        /**
         * Kiểm tra delta điều chỉnh có giá trị và nằm trong giới hạn hay không.
         *
         * @param value delta do client gửi, có thể âm để giảm
         * @param maximumMagnitude độ lớn tuyệt đối tối đa được phép
         * @return delta hợp lệ để áp dụng vào state
         */
        private int requireDelta(
                Integer value,
                int maximumMagnitude
        ) {
            // Không nhận null, 0 hoặc giá trị vượt giới hạn để tránh command vô nghĩa/nguy hiểm.
            if (value == null || value == 0 || Math.abs((long) value) > maximumMagnitude) {
                throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            }
            return value;
        }

        /**
         * Chuẩn hóa và cập nhật tên một võ sĩ.
         *
         * @param side bên red hoặc blue cần đổi tên
         * @param value tên mới do operator gửi
         * @return snapshot mới sau khi đổi tên
         */
        private Snapshot setCompetitorName(
                String side,
                String value
        ) {
            // Từ chối null trước khi gọi strip để tránh NullPointerException.
            if (value == null) throw new ApiException(SparringErrorCode.COMMAND_INVALID);

            // Bỏ khoảng trắng thừa ở đầu/cuối để state hiển thị nhất quán.
            String normalized = value.strip();

            // Không cho phép tên rỗng hoặc dài quá giới hạn giao diện/protocol.
            if (normalized.isEmpty() || normalized.length() > MAX_COMPETITOR_NAME_LENGTH) {
                throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            }
            if ("red".equals(side)) return state = state.withRedName(normalized);
            if ("blue".equals(side)) return state = state.withBlueName(normalized);
            throw new ApiException(SparringErrorCode.COMMAND_INVALID);
        }

        /**
         * Cộng hoặc trừ điểm cho một bên.
         *
         * @param side bên red hoặc blue cần thay đổi điểm
         * @param amount số điểm thay đổi, có thể âm khi operator giảm điểm
         * @return snapshot mới sau khi cập nhật điểm
         */
        private Snapshot changeScore(
                String side,
                int amount
        ) {
            // Không cho sửa điểm sau khi trận đã kết thúc.
            if (state.ended()) throw new ApiException(SparringErrorCode.COMMAND_INVALID);

            // Math.max giữ điểm không âm khi operator điều chỉnh giảm.
            if ("red".equals(side)) return state = state.withRedScore(Math.max(
                    0,
                    state.redScore() + amount
            ));
            if ("blue".equals(side)) return state = state.withBlueScore(Math.max(
                    0,
                    state.blueScore() + amount
            ));
            throw new ApiException(SparringErrorCode.COMMAND_INVALID);
        }

        /**
         * Cộng hoặc trừ penalty cho một bên.
         *
         * @param side bên red hoặc blue cần thay đổi penalty
         * @param amount số penalty thay đổi, có thể âm khi operator giảm
         * @return snapshot mới sau khi cập nhật penalty
         */
        private Snapshot changePenalty(
                String side,
                int amount
        ) {
            // Penalty không bị ràng buộc bởi ended trong implementation hiện tại,
            // nhưng vẫn được giữ không âm để state không bị sai lệch.
            if ("red".equals(side)) return state = state.withRedPenalties(Math.max(
                    0,
                    state.redPenalties() + amount
            ));
            if ("blue".equals(side)) return state = state.withBluePenalties(Math.max(
                    0,
                    state.bluePenalties() + amount
            ));
            throw new ApiException(SparringErrorCode.COMMAND_INVALID);
        }

        /**
         * Đổi số hiệp hiện tại.
         *
         * @param value số hiệp mới, chỉ chấp nhận 1 đến 3
         * @return snapshot mới sau khi đổi hiệp
         */
        private Snapshot setRound(Integer value) {
            // Chỉ chấp nhận round trong phạm vi 1 đến 3.
            if (value == null || value < 1 || value > 3) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            return state = state.withRound(value);
        }

        /**
         * Đặt lại thời gian còn lại của room.
         *
         * @param value thời lượng mới theo giây, từ 0 đến 599
         * @return snapshot mới với timer đã đặt và trạng thái pause
         */
        private Snapshot setDuration(Integer value) {
            // Giới hạn timer để tránh giá trị bất thường và luôn pause sau khi cấu hình.
            if (value == null || value < 0 || value > 599) throw new ApiException(SparringErrorCode.COMMAND_INVALID);
            return state = state.withRemainingSeconds(value).withRunning(false);
        }
    }

    private record Snapshot(
            // Mã phòng dùng để định tuyến snapshot tới đúng nhóm client.
            String roomCode,
            // Tên hiển thị của võ sĩ bên đỏ.
            String redName,
            // Tên hiển thị của võ sĩ bên xanh.
            String blueName,
            // Số hiệp hiện tại của trận.
            int round,
            // Số giây còn lại; đây là giá trị authoritative từ backend.
            int remainingSeconds,
            // Tổng điểm hiện tại của bên đỏ.
            int redScore,
            // Tổng điểm hiện tại của bên xanh.
            int blueScore,
            // Số penalty hiện tại của bên đỏ.
            int redPenalties,
            // Số penalty hiện tại của bên xanh.
            int bluePenalties,
            // true khi timer đang chạy.
            boolean running,
            // true khi operator đã kết thúc trận.
            boolean ended
    ) {
        /**
         * Các with-method tạo state mới bất biến thay vì sửa trực tiếp record hiện tại.
         * Cách này giúp mỗi snapshot đại diện cho một trạng thái hoàn chỉnh và an toàn
         * khi so sánh trước/sau hoặc gửi cho nhiều client.
         */
        // Tạo snapshot mới với tên bên đỏ được thay thế.
        Snapshot withRedName(String value) {
            return new Snapshot(
                    roomCode,
                    value,
                    blueName,
                    round,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với tên bên xanh được thay thế.
        Snapshot withBlueName(String value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    value,
                    round,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với số hiệp được thay thế.
        Snapshot withRound(int value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    value,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với số giây còn lại được thay thế.
        Snapshot withRemainingSeconds(int value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    value,
                    redScore,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với điểm bên đỏ được thay thế.
        Snapshot withRedScore(int value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    remainingSeconds,
                    value,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với điểm bên xanh được thay thế.
        Snapshot withBlueScore(int value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    remainingSeconds,
                    redScore,
                    value,
                    redPenalties,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với penalty bên đỏ được thay thế.
        Snapshot withRedPenalties(int value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    value,
                    bluePenalties,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với penalty bên xanh được thay thế.
        Snapshot withBluePenalties(int value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    redPenalties,
                    value,
                    running,
                    ended
            );
        }

        // Tạo snapshot mới với trạng thái đang chạy/tạm dừng được thay thế.
        Snapshot withRunning(boolean value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    value,
                    ended
            );
        }

        // Tạo snapshot mới với trạng thái đã kết thúc được thay thế.
        Snapshot withEnded(boolean value) {
            return new Snapshot(
                    roomCode,
                    redName,
                    blueName,
                    round,
                    remainingSeconds,
                    redScore,
                    blueScore,
                    redPenalties,
                    bluePenalties,
                    running,
                    value
            );
        }
    }
}
