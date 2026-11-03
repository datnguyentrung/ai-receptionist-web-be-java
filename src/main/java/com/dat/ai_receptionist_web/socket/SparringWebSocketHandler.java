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
/**
 * Handler xử lý toàn bộ kết nối WebSocket của tính năng Đối kháng.
 *
 * <p>Class này là adapter giữa giao thức WebSocket và các module nghiệp vụ:
 * xác thực ticket, đăng ký session vào room, chuyển JSON command tới
 * {@link SparringRoomRegistry}, sau đó broadcast snapshot mới cho các client
 * đang cùng tham gia room.</p>
 */
public class SparringWebSocketHandler extends TextWebSocketHandler {
    // ObjectMapper chuyển JSON message thành JsonNode và chuyển response thành chuỗi JSON.
    private final ObjectMapper objectMapper;

    // Registry chứa state và business rule của các room Đối kháng.
    private final SparringRoomRegistry rooms;

    // Service phát hành/kiểm tra ticket dùng để xác thực handshake WebSocket.
    private final SparringSocketTicketService tickets;

    // Danh sách session đang kết nối, được nhóm theo roomCode.
    // ConcurrentHashMap giúp map an toàn khi nhiều client kết nối/ngắt đồng thời.
    private final Map<String, Set<WebSocketSession>> sessionsByRoom = new ConcurrentHashMap<>();

    /**
     * Đăng ký listener nhận thay đổi timer từ room registry.
     *
     * <p>Timer được cập nhật bên trong {@code SparringRoomRegistry}. Khi state
     * thay đổi, callback này chuyển snapshot thành WebSocket event và gửi cho
     * toàn bộ client trong room tương ứng.</p>
     */
    @jakarta.annotation.PostConstruct
        // Đăng ký listener để timer trong Registry có thể đẩy snapshot mới xuống client.
    void subscribeToTicks() {
        // Registry gọi callback này mỗi khi tick làm thay đổi snapshot.
        rooms.onSnapshot(snapshot -> {
            try {
                // Broadcast event snapshot tới các client đang ở cùng room.
                broadcast(
                        snapshot.roomCode(),
                        "snapshot",
                        snapshot
                );
            } catch (IOException ignored) {
                // Một client lỗi khi gửi không làm scheduler/timer của registry bị dừng.
            }
        });
    }

    /**
     * Xử lý một WebSocket connection ngay sau khi handshake thành công.
     *
     * @param session session đại diện cho client vừa kết nối
     * @throws IOException nếu không thể gửi snapshot hoặc đóng session
     *
     * <p>Luồng chính: lấy ticket từ URL, xác thực ticket, kiểm tra room,
     * lưu metadata quyền vào session, đăng ký session vào room và gửi snapshot
     * hiện tại để client khởi tạo giao diện.</p>
     */
    @Override
    // Xác thực ticket khi handshake, lưu metadata room/operator và gửi snapshot đầu tiên.
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        try {
            // Đọc giá trị ticket từ query parameter trong URL handshake.
            String ticketValue = ticketFrom(session.getUri());

            // Resolve kiểm tra ticket có tồn tại và còn hạn hay không.
            SparringSocketTicketService.Ticket ticket = tickets.resolve(ticketValue);

            // Ticket của chế độ tập không được phép đi vào socket Đối kháng thật.
            if (!"sparring".equals(ticket.roomType())) throw new IllegalStateException("Wrong room type");

            // Ticket hợp lệ nhưng room đã mất thì không được phép tham gia.
            if (!rooms.exists(ticket.roomCode())) throw new IllegalStateException("Room no longer exists");

            // Lưu roomCode vào session để các message sau biết phải cập nhật room nào.
            session.getAttributes().put(
                    "roomCode",
                    ticket.roomCode()
            );

            // Lưu quyền operator vào session để không phải đọc lại JWT ở mỗi command.
            session.getAttributes().put(
                    "operator",
                    ticket.operator()
            );

            // Lấy hoặc tạo tập session của room rồi đăng ký client hiện tại vào tập đó.
            sessionsByRoom.computeIfAbsent(
                    ticket.roomCode(),
                    ignored -> ConcurrentHashMap.newKeySet()
            ).add(session);

            // Gửi snapshot hiện tại ngay lập tức để client không phải chờ command/tick đầu tiên.
            send(
                    session,
                    "snapshot",
                    rooms.get(ticket.roomCode())
            );
        } catch (Exception error) {
            // Mọi lỗi handshake được gửi dưới dạng event error trước khi đóng session.
            sendError(
                    session,
                    error
            );

            // POLICY_VIOLATION báo cho client rằng kết nối bị từ chối do lỗi policy/ticket.
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    /**
     * Nhận và xử lý một text message từ client.
     *
     * @param session session đã được đăng ký vào room
     * @param message message JSON chứa command và các tham số tùy chọn
     * @throws IOException nếu không thể broadcast hoặc gửi error response
     *
     * <p>Command được parse thành các field đơn giản rồi chuyển cho Registry.
     * Registry mới là nơi kiểm tra quyền và áp dụng business rule; handler chỉ
     * chịu trách nhiệm chuyển tiếp và phát kết quả.</p>
     */
    @Override
    // Đọc command JSON từ client, áp dụng vào room rồi broadcast trạng thái mới.
    protected void handleTextMessage(
            @NonNull WebSocketSession session,
            @NonNull TextMessage message
    ) throws IOException {
        // Lấy roomCode đã lưu lúc handshake; session chưa xác thực thì bỏ qua message.
        String roomCode = (String) session.getAttributes().get("roomCode");
        if (roomCode == null) return;
        try {
            // Parse payload JSON thành cây JsonNode để đọc field linh hoạt.
            JsonNode payload = objectMapper.readTree(message.getPayload());

            // type xác định command, ví dụ judgeScore, start, pause hoặc end.
            String type = payload.path("type").asText();

            // Registry xử lý command và trả về snapshot sau khi cập nhật thành công.
            // Boolean.TRUE.equals tránh lỗi khi attribute operator bị thiếu hoặc null.
            SparringRoomResponse snapshot = rooms.apply(
                    roomCode,
                    Boolean.TRUE.equals(session.getAttributes().get("operator")),
                    type,
                    nullableText(
                            payload,
                            "side"
                    ),
                    nullableInt(
                            payload,
                            "points"
                    ),
                    nullableInt(
                            payload,
                            "delta"
                    ),
                    nullableInt(
                            payload,
                            "seconds"
                    ),
                    nullableInt(
                            payload,
                            "round"
                    ),
                    nullableText(
                            payload,
                            "name"
                    )
            );

            // Đồng bộ state mới cho mọi client đang ở cùng room.
            broadcast(
                    roomCode,
                    "snapshot",
                    snapshot
            );
        } catch (Exception error) {
            // Lỗi parse hoặc lỗi nghiệp vụ chỉ trả về client gửi command,
            // không làm đóng toàn bộ room hay các session khác.
            sendError(
                    session,
                    error
            );
        }
    }

    /**
     * Hủy đăng ký session khi client đóng WebSocket.
     *
     * @param session session vừa đóng
     * @param status lý do/trạng thái đóng kết nối do framework cung cấp
     *
     * <p>Nếu room không còn session nào, xóa luôn entry của room khỏi map
     * để tránh giữ collection rỗng không cần thiết.</p>
     */
    @Override
    // Gỡ session khỏi danh sách của room khi client đóng kết nối.
    public void afterConnectionClosed(
            @NonNull WebSocketSession session,
            @NonNull CloseStatus status
    ) {
        // Attribute roomCode được lưu trong afterConnectionEstablished().
        Object roomCode = session.getAttributes().get("roomCode");
        if (roomCode instanceof String value) {
            // Lấy danh sách session của room; null nghĩa là room đã được dọn trước đó.
            Set<WebSocketSession> sessions = sessionsByRoom.get(value);
            if (sessions != null) {
                // Xóa session hiện tại để không broadcast cho client đã rời đi.
                sessions.remove(session);

                // Nếu không còn client nào, xóa room khỏi map session.
                if (sessions.isEmpty()) sessionsByRoom.remove(
                        value,
                        sessions
                );
            }
        }
    }

    /**
     * Broadcast một event tới các session đang thuộc một room.
     *
     * @param roomCode room nhận event
     * @param type loại event, thường là snapshot hoặc error
     * @param data payload đi kèm event
     * @throws IOException nếu một lần gửi message thất bại
     *
     * <p>Chỉ gửi cho session còn mở. Việc đóng session được xử lý ở lifecycle
     * callback của WebSocket.</p>
     */
    private void broadcast(
            String roomCode,
            String type,
            Object data
    ) throws IOException {
        // Nếu room chưa có client, dùng Set.of() thay vì null để vòng lặp an toàn.
        Set<WebSocketSession> sessions = sessionsByRoom.getOrDefault(
                roomCode,
                Set.of()
        );

        // Gửi event lần lượt tới các session còn đang mở.
        for (WebSocketSession session : sessions)
            if (session.isOpen()) send(
                    session,
                    type,
                    data
            );
    }

    /**
     * Đóng gói và gửi một event JSON tới một client.
     *
     * @param session session đích
     * @param type loại event để client phân biệt cách xử lý
     * @param data dữ liệu event, thường là snapshot hoặc object lỗi
     * @throws IOException nếu framework không gửi được message
     */
    private void send(
            WebSocketSession session,
            String type,
            Object data
    ) throws IOException {
        // Map.of tạo envelope thống nhất: { "type": ..., "data": ... }.
        // ObjectMapper biến envelope thành chuỗi JSON rồi TextMessage gửi qua WebSocket.
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type",
                type,
                "data",
                data
        ))));
    }

    /**
     * Chuyển exception thành event error có format ổn định cho client.
     *
     * @param session client cần nhận thông báo lỗi
     * @param error exception phát sinh khi handshake hoặc xử lý command
     * @throws IOException nếu không gửi được event lỗi
     *
     * <p>ApiException được dùng detail nghiệp vụ thân thiện; lỗi kỹ thuật
     * khác dùng message tổng quát để không làm lộ chi tiết nội bộ.</p>
     */
    private void sendError(
            WebSocketSession session,
            Exception error
    ) throws IOException {
        // Chỉ lấy detail nghiệp vụ với ApiException; lỗi khác dùng thông báo chung.
        String message = error instanceof ApiException api ? api.responseDetail() : "Unable to process sparring command";

        // Client nhận event { type: "error", data: { message: ... } }.
        send(
                session,
                "error",
                Map.of(
                        "message",
                        message
                )
        );
    }

    /**
     * Lấy socket ticket từ query parameter của URL handshake.
     *
     * @param uri URI client dùng để mở WebSocket
     * @return giá trị ticket không rỗng
     * @throws IllegalArgumentException nếu URI không có ticket hợp lệ
     */
    private String ticketFrom(URI uri) {
        // UriComponentsBuilder giúp đọc query parameter mà không phải tự cắt chuỗi URL.
        String ticket = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("ticket");

        // Không có ticket thì không thể xác định client đã được cấp quyền vào room.
        if (ticket == null || ticket.isBlank()) throw new IllegalArgumentException("Missing socket ticket");
        return ticket;
    }

    /**
     * Đọc một field số nguyên tùy chọn từ JSON payload.
     *
     * @param node payload JSON cần đọc
     * @param name tên field cần lấy
     * @return số nguyên nếu field tồn tại và khác null; ngược lại null
     */
    private static Integer nullableInt(
            JsonNode node,
            String name
    ) {
        // hasNonNull phân biệt field có giá trị với field thiếu/null,
        // giúp Registry nhận đúng tham số optional của command.
        return node.hasNonNull(name) ? node.get(name).asInt() : null;
    }

    /**
     * Đọc một field chuỗi tùy chọn từ JSON payload.
     *
     * @param node payload JSON cần đọc
     * @param name tên field cần lấy
     * @return chuỗi nếu field tồn tại và khác null; ngược lại null
     */
    private static String nullableText(
            JsonNode node,
            String name
    ) {
        // Trả null khi field không có để business rule tự quyết định hợp lệ hay không.
        return node.hasNonNull(name) ? node.get(name).asText() : null;
    }
}
