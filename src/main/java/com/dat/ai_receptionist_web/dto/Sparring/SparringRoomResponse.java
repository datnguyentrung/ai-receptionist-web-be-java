package com.dat.ai_receptionist_web.dto.Sparring;

public record SparringRoomResponse(String roomCode, Snapshot snapshot) {
    public record Snapshot(
            String roomCode,
            int round,
            int remainingSeconds,
            int redScore,
            int blueScore,
            int redPenalties,
            int bluePenalties,
            boolean running,
            boolean ended
    ) { }
}
