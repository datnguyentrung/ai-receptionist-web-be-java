package com.dat.ai_receptionist_web.dto.Sparring;

import java.util.List;

/** Snapshot duy nhất mà REST và WebSocket dùng để đồng bộ buổi tập trọng tài. */
public record SparringPracticeRoomResponse(String roomCode, Snapshot snapshot) {
    public record Snapshot(
            String roomCode,
            int maximumJudges,
            int consensusJudges,
            int scoringWindowSeconds,
            int redScore,
            int blueScore,
            String status,
            long countdownEndsAtEpochMs,
            long mediaOffsetMs,
            String videoUrl,
            boolean videoUploaded,
            boolean canStart,
            List<Judge> judges
    ) { }

    /** Không trả userId ra client; client chỉ cần vị trí, kết nối và readiness. */
    public record Judge(int position, boolean connected, boolean ready) { }
}
