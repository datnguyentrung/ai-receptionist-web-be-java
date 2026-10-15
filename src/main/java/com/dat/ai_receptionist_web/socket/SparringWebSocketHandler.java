package com.dat.ai_receptionist_web.socket;

import com.dat.ai_receptionist_web.dto.Sparring.SparringRoomResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.service.Sparring.SparringRoomRegistry;
import com.dat.ai_receptionist_web.service.Sparring.SparringSocketTicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SparringWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final SparringRoomRegistry rooms;
    private final SparringSocketTicketService tickets;
    private final Map<String, Set<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    void subscribeToTicks() {
        rooms.onSnapshot(snapshot -> {
            try { broadcast(snapshot.roomCode(), "snapshot", snapshot); }
            catch (IOException ignored) { }
        });
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        try {
            String ticketValue = ticketFrom(session.getUri());
            SparringSocketTicketService.Ticket ticket = tickets.resolve(ticketValue);
            if (!rooms.exists(ticket.roomCode())) throw new IllegalStateException("Room no longer exists");
            session.getAttributes().put("roomCode", ticket.roomCode());
            session.getAttributes().put("operator", ticket.operator());
            sessionsByRoom.computeIfAbsent(ticket.roomCode(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
            send(session, "snapshot", rooms.get(ticket.roomCode()));
        } catch (Exception error) {
            sendError(session, error);
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws IOException {
        String roomCode = (String) session.getAttributes().get("roomCode");
        if (roomCode == null) return;
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();
            SparringRoomResponse snapshot = rooms.apply(roomCode, Boolean.TRUE.equals(session.getAttributes().get("operator")), type,
                    nullableText(payload, "side"), nullableInt(payload, "points"), nullableInt(payload, "delta"),
                    nullableInt(payload, "seconds"), nullableInt(payload, "round"));
            broadcast(roomCode, "snapshot", snapshot);
        } catch (Exception error) {
            sendError(session, error);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        Object roomCode = session.getAttributes().get("roomCode");
        if (roomCode instanceof String value) {
            Set<WebSocketSession> sessions = sessionsByRoom.get(value);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) sessionsByRoom.remove(value, sessions);
            }
        }
    }

    private void broadcast(String roomCode, String type, Object data) throws IOException {
        Set<WebSocketSession> sessions = sessionsByRoom.getOrDefault(roomCode, Set.of());
        for (WebSocketSession session : sessions) if (session.isOpen()) send(session, type, data);
    }
    private void send(WebSocketSession session, String type, Object data) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", type, "data", data))));
    }
    private void sendError(WebSocketSession session, Exception error) throws IOException {
        String message = error instanceof ApiException api ? api.responseDetail() : "Unable to process sparring command";
        send(session, "error", Map.of("message", message));
    }
    private String ticketFrom(URI uri) {
        String ticket = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("ticket");
        if (ticket == null || ticket.isBlank()) throw new IllegalArgumentException("Missing socket ticket");
        return ticket;
    }
    private static Integer nullableInt(JsonNode node, String name) { return node.hasNonNull(name) ? node.get(name).asInt() : null; }
    private static String nullableText(JsonNode node, String name) { return node.hasNonNull(name) ? node.get(name).asText() : null; }
}
