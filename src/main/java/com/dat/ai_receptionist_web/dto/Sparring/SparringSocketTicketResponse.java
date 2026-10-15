package com.dat.ai_receptionist_web.dto.Sparring;

public record SparringSocketTicketResponse(String roomCode, String ticket, boolean operator, long expiresAtEpochMs) { }
