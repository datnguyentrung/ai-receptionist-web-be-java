package com.dat.ai_receptionist_web.dto.Sparring;

import java.util.List;

/**
 * Snapshot duy nhất mà REST và WebSocket dùng để đồng bộ buổi tập trọng tài.
 */
public record SparringPracticeRoomResponse(
        String roomCode,
        Snapshot snapshot
) {

    public record Snapshot(
            // Mã phòng tập
            String roomCode,

            // Số trọng tài tối đa
            int maximumJudges,

            // Số trọng tài cần đồng thuận
            int consensusJudges,

            // Thời gian cho phép chấm điểm (giây)
            int scoringWindowSeconds,

            // Điểm của vận động viên đỏ
            int redScore,

            // Điểm của vận động viên xanh
            int blueScore,

            // Trạng thái buổi tập
            String status,

            // Thời điểm kết thúc đếm ngược (Unix milliseconds)
            long countdownEndsAtEpochMs,

            // Độ lệch thời gian phát video (milliseconds)
            long mediaOffsetMs,

            // URL video của buổi tập
            String videoUrl,

            // Video đã được tải lên hay chưa
            boolean videoUploaded,

            // Buổi tập đã đủ điều kiện bắt đầu hay chưa
            boolean canStart,

            // Danh sách trạng thái các trọng tài
            List<Judge> judges
    ) {
    }

    /**
     * Không trả userId ra client; client chỉ cần vị trí, kết nối và readiness.
     */
    public record Judge(
            // Vị trí trọng tài
            int position,

            // Trọng tài đang kết nối hay không
            boolean connected,

            // Trọng tài đã sẵn sàng hay chưa
            boolean ready
    ) {
    }
}