package com.dat.ai_receptionist_web.dto.Sparring;

// DTO bất biến dùng để trả snapshot phòng ra REST API và WebSocket.
public record SparringRoomResponse(String roomCode, Snapshot snapshot) {
    // Dữ liệu hiển thị hiện tại của một trận đấu; DTO không chứa business behavior.
    public record Snapshot(
            String roomCode,
            String redName,
            String blueName,
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
