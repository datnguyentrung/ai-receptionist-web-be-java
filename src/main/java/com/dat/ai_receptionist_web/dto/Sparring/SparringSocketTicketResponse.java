package com.dat.ai_receptionist_web.dto.Sparring;

// DTO trả ticket và metadata quyền để client mở WebSocket vào đúng phòng.
public record SparringSocketTicketResponse(
        String roomCode,
        String ticket,
        boolean operator,
        long expiresAtEpochMs
) { }
