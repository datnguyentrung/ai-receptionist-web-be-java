package com.dat.ai_receptionist_web.client;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated internal gateway client forwarding verified JWT tenant & user context to be-py.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PythonAiGatewayClient {

    @Value("${python-backend.api:${BACKEND_PYTHON_API:}}")
    private String backendPythonApi;

    @Value("${python-backend.internal-token:${PYTHON_INTERNAL_AUTH_TOKEN:${HUGGING_FACE_TOKEN:}}}")
    private String internalToken;

    @Value("${python-backend.connect-timeout:${PYTHON_BACKEND_CONNECT_TIMEOUT:5s}}")
    private Duration connectTimeout;

    @Value("${python-backend.read-timeout:${PYTHON_BACKEND_READ_TIMEOUT:120s}}")
    private Duration readTimeout;

    private RestClient restClient;

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(backendPythonApi)) {
            log.warn("PythonAiGatewayClient disabled: BACKEND_PYTHON_API is not set");
            return;
        }
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeout);
        rf.setReadTimeout(readTimeout);
        String base = backendPythonApi.endsWith("/")
                ? backendPythonApi.substring(0, backendPythonApi.length() - 1)
                : backendPythonApi;
        restClient = RestClient.builder()
                .baseUrl(base)
                .requestFactory(rf)
                .build();
    }

    public ResponseEntity<String> forwardJson(HttpMethod method, String path, Object body) {
        requireConfigured();
        RestClient.RequestBodySpec spec = restClient
                .method(method)
                .uri(path)
                .headers(this::applyTrustedContextHeaders)
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            spec.body(body);
        }
        // Gateway responses must preserve an upstream 4xx (notably the normal
        // "no published form yet" 404) instead of turning it into a Java 500.
        try {
            return spec.retrieve().toEntity(String.class);
        } catch (RestClientResponseException exception) {
            MediaType contentType = exception.getResponseHeaders() == null
                    ? MediaType.APPLICATION_JSON
                    : exception.getResponseHeaders().getContentType();
            return ResponseEntity.status(exception.getStatusCode())
                    .contentType(contentType == null ? MediaType.APPLICATION_JSON : contentType)
                    .body(exception.getResponseBodyAsString());
        }
    }

    public ResponseEntity<String> forwardDocument(
            String path, String scopeKey, String title, MultipartFile file
    ) throws IOException {
        requireConfigured();
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("scope_key", scopeKey);
        parts.add("title", title == null ? "" : title);
        parts.add("file", toFilePart(file));
        return restClient.post()
                .uri(path)
                .headers(this::applyTrustedContextHeaders)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .toEntity(String.class);
    }

    public void streamJsonSse(String path, Object body, OutputStream out) {
        requireConfigured();
        restClient.post()
                .uri(path)
                .headers(this::applyTrustedContextHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((req, res) -> {
                    StreamUtils.copy(res.getBody(), out);
                    out.flush();
                    return null;
                });
    }

    /**
     * Server-side proxy for an unauthenticated visitor.  The public controller
     * resolves the tenant and derives the guest identity; browser supplied
     * tenant/user headers are never forwarded to Python.
     */
    public void streamGuestJsonSse(
            String path,
            Object body,
            UUID tenantId,
            String guestUserId,
            OutputStream out
    ) {
        requireConfigured();
        restClient.post()
                .uri(path)
                .headers(headers -> applyTrustedContextHeaders(headers, tenantId, guestUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((req, res) -> {
                    StreamUtils.copy(res.getBody(), out);
                    out.flush();
                    return null;
                });
    }

    public void streamMultipartSse(
            String path,
            String message,
            String sessionId,
            boolean streamTokens,
            List<MultipartFile> files,
            OutputStream out
    ) throws IOException {
        requireConfigured();
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("message", message == null ? "" : message);
        if (StringUtils.hasText(sessionId)) {
            parts.add("session_id", sessionId);
        }
        parts.add("stream_tokens", Boolean.toString(streamTokens));
        if (files != null) {
            for (MultipartFile file : files) {
                parts.add("files", toFilePart(file));
            }
        }
        restClient.post()
                .uri(path)
                .headers(this::applyTrustedContextHeaders)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(parts)
                .exchange((req, res) -> {
                    StreamUtils.copy(res.getBody(), out);
                    out.flush();
                    return null;
                });
    }

    private void applyTrustedContextHeaders(HttpHeaders headers) {
        if (!StringUtils.hasText(internalToken)) {
            throw new IllegalStateException("PYTHON_INTERNAL_AUTH_TOKEN is required (fail-closed)");
        }
        UUID tenantId = TenantContext.getTenantId()
                .or(SecurityUtil::getCurrentTenantId)
                .orElseThrow(() -> new IllegalStateException("Authenticated tenant context is required"));
        UUID userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("Authenticated user is required"));

        applyTrustedContextHeaders(headers, tenantId, userId.toString());

        SecurityUtil.getJwt().ifPresent(jwt -> {
            headers.set("X-User-Roles", joinClaim(jwt, "roles"));
            headers.set("X-User-Permissions", joinClaim(jwt, "permissions"));
            if (StringUtils.hasText(jwt.getTokenValue())) {
                headers.set("X-User-Access-Token", jwt.getTokenValue());
            }
        });
    }

    private void applyTrustedContextHeaders(HttpHeaders headers, UUID tenantId, String userId) {
        if (!StringUtils.hasText(internalToken)) {
            throw new IllegalStateException("PYTHON_INTERNAL_AUTH_TOKEN is required (fail-closed)");
        }
        headers.set("X-Internal-Service-Token", internalToken.trim());
        headers.set("X-Tenant-Id", tenantId.toString());
        headers.set("X-User-Id", userId);
    }

    private static String joinClaim(Jwt jwt, String claimName) {
        List<String> items = jwt.getClaimAsStringList(claimName);
        return items == null ? "" : String.join(",", items);
    }

    private static HttpEntity<ByteArrayResource> toFilePart(MultipartFile file) throws IOException {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(
                StringUtils.hasText(file.getContentType())
                        ? MediaType.parseMediaType(file.getContentType())
                        : MediaType.APPLICATION_OCTET_STREAM
        );
        String filename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "upload.bin";
        partHeaders.setContentDispositionFormData("file", filename);
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return new HttpEntity<>(resource, partHeaders);
    }

    private void requireConfigured() {
        if (restClient == null) {
            throw new IllegalStateException("PythonAiGatewayClient is not configured (missing BACKEND_PYTHON_API)");
        }
    }
}
