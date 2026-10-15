package com.dat.ai_receptionist_web.service.Sparring;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SparringErrorCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SparringSocketTicketService {
    private static final long TICKET_TTL_MS = 60_000;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

    public IssuedTicket issue(String roomCode, UUID userId, boolean operator) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expiresAt = Instant.now().toEpochMilli() + TICKET_TTL_MS;
        tickets.put(value, new Ticket(roomCode, userId, operator, expiresAt));
        return new IssuedTicket(value, expiresAt);
    }

    public Ticket resolve(String ticket) {
        Ticket value = tickets.get(ticket);
        if (value == null || value.expiresAtEpochMs() < Instant.now().toEpochMilli()) {
            tickets.remove(ticket);
            throw new ApiException(SparringErrorCode.SOCKET_TICKET_INVALID);
        }
        return value;
    }

    public record Ticket(String roomCode, UUID userId, boolean operator, long expiresAtEpochMs) { }
    public record IssuedTicket(String value, long expiresAtEpochMs) { }
}
