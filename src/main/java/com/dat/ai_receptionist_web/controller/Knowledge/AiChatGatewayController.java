package com.dat.ai_receptionist_web.controller.Knowledge;

import com.dat.ai_receptionist_web.client.PythonAiGatewayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Authenticated SSE Chat Gateway proxying mobile AI chat to be-py with verified tenant context.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatGatewayController {

    private final PythonAiGatewayClient gatewayClient;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamingResponseBody> streamChat(@RequestBody Map<String, Object> body) {
        StreamingResponseBody stream = out -> gatewayClient.streamJsonSse("/api/v1/agent/chat/stream", body, out);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }

    @PostMapping(
            value = "/chat/upload/stream",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamingResponseBody> streamChatWithUpload(
            @RequestParam(value = "message", defaultValue = "") String message,
            @RequestParam(value = "session_id", required = false) String sessionId,
            @RequestParam(value = "stream_tokens", defaultValue = "true") boolean streamTokens,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        StreamingResponseBody stream = out ->
                gatewayClient.streamMultipartSse("/api/v1/agent/chat/upload/stream", message, sessionId, streamTokens, files, out);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }
}
