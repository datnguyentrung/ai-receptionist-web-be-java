package com.dat.ai_receptionist_web.controller.Knowledge;

import com.dat.ai_receptionist_web.client.PythonAiGatewayClient;
import com.dat.ai_receptionist_web.domain.Core.Tenant;
import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import com.dat.ai_receptionist_web.repository.Core.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Public, tenant-scoped text chat. Python remains reachable only through this gateway. */
@RestController
@RequestMapping("/api/v1/public/tenants")
@RequiredArgsConstructor
public class PublicAiChatGatewayController {

    private final PythonAiGatewayClient gatewayClient;
    private final TenantRepository tenantRepository;

    @PostMapping(value = "/{tenantId}/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamGuestChat(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> body
    ) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .filter(value -> value.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Trung tâm không tồn tại hoặc đã ngưng hoạt động."));

        String message = String.valueOf(body.getOrDefault("message", "")).trim();
        if (message.isEmpty() || message.length() > 1_000) {
            throw new ResponseStatusException(BAD_REQUEST, "Nội dung chat phải có từ 1 đến 1000 ký tự.");
        }

        String guestSessionId = normalizeGuestSessionId(tenantId, body.get("session_id"));
        Map<String, Object> sanitizedBody = new LinkedHashMap<>();
        sanitizedBody.put("message", message);
        sanitizedBody.put("session_id", guestSessionId);
        sanitizedBody.put("attachments", java.util.List.of());
        sanitizedBody.put("stream_tokens", false);
        String guestUserId = "guest-" + guestSessionId;

        StreamingResponseBody stream = out -> gatewayClient.streamGuestJsonSse(
                "/api/v1/agent/chat/stream", sanitizedBody, tenant.getTenantId(), guestUserId, out
        );
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(stream);
    }

    private static String normalizeGuestSessionId(UUID tenantId, Object rawSessionId) {
        String value = rawSessionId == null ? "" : String.valueOf(rawSessionId).trim();
        String prefix = tenantId + ":";
        if (value.startsWith(prefix)) value = value.substring(prefix.length());
        if (value.isBlank()) return UUID.randomUUID().toString();
        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Mã phiên khách không hợp lệ.");
        }
    }
}
