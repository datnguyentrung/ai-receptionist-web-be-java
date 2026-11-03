package com.dat.ai_receptionist_web.socket;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.service.Sparring.SparringPracticeRoomRegistry;
import com.dat.ai_receptionist_web.service.Sparring.SparringSocketTicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Transport-only handler: registry giữ luật room, lớp này giữ session/broadcast. */
@Component
@RequiredArgsConstructor
public class SparringPracticeWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final SparringPracticeRoomRegistry rooms;
    private final SparringSocketTicketService tickets;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByRoom=new ConcurrentHashMap<>();

    @PostConstruct void subscribe() { rooms.onSnapshot(snapshot -> { try { broadcast(snapshot.roomCode(),"snapshot",snapshot); } catch (IOException ignored) { } }); }

    @Override public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        try {
            SparringSocketTicketService.Ticket ticket=tickets.resolve(ticketFrom(session.getUri()));
            if (!"practice".equals(ticket.roomType()) || !rooms.exists(ticket.roomCode())) throw new IllegalArgumentException("Invalid practice ticket");
            session.getAttributes().put("roomCode",ticket.roomCode()); session.getAttributes().put("userId",ticket.userId()); session.getAttributes().put("operator",ticket.operator());
            rooms.connected(ticket.roomCode(),ticket.userId(),ticket.operator());
            sessionsByRoom.computeIfAbsent(ticket.roomCode(), ignored -> ConcurrentHashMap.newKeySet()).add(session);
            send(session,"snapshot",rooms.get(ticket.roomCode()));
        } catch (Exception error) { sendError(session,error); session.close(CloseStatus.POLICY_VIOLATION); }
    }

    @Override protected void handleTextMessage(@NonNull WebSocketSession session,@NonNull TextMessage message) throws IOException {
        String roomCode=(String)session.getAttributes().get("roomCode"); UUID userId=(UUID)session.getAttributes().get("userId");
        if(roomCode==null||userId==null) return;
        try { JsonNode payload=objectMapper.readTree(message.getPayload()); rooms.command(roomCode,userId,Boolean.TRUE.equals(session.getAttributes().get("operator")),payload.path("type").asText(),text(payload,"side"),integer(payload,"points")); }
        catch(Exception error){sendError(session,error);}
    }

    @Override public void afterConnectionClosed(@NonNull WebSocketSession session,@NonNull CloseStatus status) {
        Object room=session.getAttributes().get("roomCode"); Object user=session.getAttributes().get("userId");
        if(room instanceof String roomCode && user instanceof UUID userId) {
            Set<WebSocketSession> sessions=sessionsByRoom.get(roomCode);
            if(sessions!=null){
                sessions.remove(session);
                // Một user có thể refresh trang: chỉ báo rớt khi không còn socket nào của user đó.
                boolean hasAnotherSession=sessions.stream().anyMatch(candidate -> userId.equals(candidate.getAttributes().get("userId")) && candidate.isOpen());
                if(!hasAnotherSession) rooms.disconnected(roomCode,userId,Boolean.TRUE.equals(session.getAttributes().get("operator")));
                if(sessions.isEmpty())sessionsByRoom.remove(roomCode,sessions);
            } else rooms.disconnected(roomCode,userId,Boolean.TRUE.equals(session.getAttributes().get("operator")));
        }
    }

    private void broadcast(String roomCode,String type,Object data) throws IOException { for(WebSocketSession session:sessionsByRoom.getOrDefault(roomCode,Set.of())) if(session.isOpen()) send(session,type,data); }
    private void send(WebSocketSession session,String type,Object data) throws IOException { session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type",type,"data",data)))); }
    private void sendError(WebSocketSession session,Exception error) throws IOException { send(session,"error",Map.of("message",error instanceof ApiException api?api.responseDetail():"Unable to process practice command")); }
    private static String ticketFrom(URI uri){String value=UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("ticket");if(value==null||value.isBlank())throw new IllegalArgumentException("Missing socket ticket");return value;}
    private static String text(JsonNode node,String field){return node.hasNonNull(field)?node.get(field).asText():null;}
    private static Integer integer(JsonNode node,String field){return node.hasNonNull(field)?node.get(field).asInt():null;}
}
