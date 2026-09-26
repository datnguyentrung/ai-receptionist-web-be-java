package com.dat.ai_receptionist_web.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PythonBackendClient {
    private static final Set<String> FACE_EMBEDDING_ERROR_CODES = Set.of(
            "INVALID_IMAGE_FILE",
            "EMPTY_IMAGE_FILE",
            "FILE_TOO_LARGE",
            "UNSUPPORTED_IMAGE_TYPE",
            "IMAGE_DECODE_FAILED",
            "FACE_NOT_DETECTED",
            "MULTIPLE_FACES_DETECTED",
            "FACE_EMBEDDING_FAILED",
            "INVALID_EMBEDDING",
            "MODEL_NOT_INITIALIZED",
            "INTERNAL_ERROR"
    );
    private static final Set<String> BUSINESS_CODE_FIELD_NAMES = Set.of(
            "code",
            "error",
            "errorCode",
            "error_code",
            "detail"
    );

    private final ObjectMapper objectMapper;

    @Value("${python-backend.api:${BACKEND_PYTHON_API:}}")
    private String backendPythonApi;

    @Value("${python-backend.hugging-face-token:${HUGGING_FACE_TOKEN:}}")
    private String huggingFaceToken;

    @Value("${python-backend.connect-timeout:${PYTHON_BACKEND_CONNECT_TIMEOUT:5s}}")
    private Duration connectTimeout;

    @Value("${python-backend.read-timeout:${PYTHON_BACKEND_READ_TIMEOUT:60s}}")
    private Duration readTimeout;

    private RestClient restClient;

    @PostConstruct
    void initializeRestClient() {
        if (!StringUtils.hasText(backendPythonApi) || !StringUtils.hasText(huggingFaceToken)) {
            log.warn("Python backend client is not configured; face embedding calls will fail until BACKEND_PYTHON_API and HUGGING_FACE_TOKEN are set");
            return;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        restClient = RestClient.builder()
                .baseUrl(stripTrailingSlash(backendPythonApi))
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .defaultHeaders(headers -> headers.setBearerAuth(huggingFaceToken.trim()))
                .build();
        log.info("Python backend client configured: connectTimeoutMs={}, readTimeoutMs={}",
                connectTimeout.toMillis(), readTimeout.toMillis());
    }

    public FaceEmbeddingResponse generateFaceEmbedding(MultipartFile file) {
        requireConfigured();
        String requestId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        String path = "/face-embeddings";
        try {
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("file", createFilePart(file));

            FaceEmbeddingResponse response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("X-Request-ID", requestId)
                    .body(requestBody)
                    .retrieve()
                    .body(FaceEmbeddingResponse.class);

            if (response == null) {
                throw new PythonBackendClientException(
                        FailureType.INVALID_RESPONSE,
                        "Python backend did not return a face embedding"
                );
            }
            if (!response.success()) {
                throw new PythonBackendClientException(
                        FailureType.REJECTED,
                        "Python backend rejected the face embedding request",
                        normalizeFaceEmbeddingErrorCode(response.errorCode()),
                        null
                );
            }
            if (!isValidEmbedding(response)) {
                throw new PythonBackendClientException(
                        FailureType.INVALID_RESPONSE,
                        "Python backend returned an invalid face embedding"
                );
            }
            log.info("Python backend face embedding completed: requestId={}, durationMs={}",
                    requestId, elapsedMillis(startedAtNanos));
            return response;
        } catch (PythonBackendClientException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw responseException(requestId, path, startedAtNanos, exception);
        } catch (ResourceAccessException exception) {
            throw resourceAccessException(requestId, path, startedAtNanos, exception);
        } catch (RestClientException exception) {
            throw invalidResponseException(requestId, path, startedAtNanos, exception);
        }
    }

    public void checkHealth() {
        requireConfigured();
        String requestId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        try {
            restClient.get()
                    .uri("/health")
                    .header("X-Request-ID", requestId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Python backend health check completed: requestId={}, durationMs={}",
                    requestId, elapsedMillis(startedAtNanos));
        } catch (RestClientResponseException exception) {
            throw responseException(requestId, "/health", startedAtNanos, exception);
        } catch (ResourceAccessException exception) {
            throw resourceAccessException(requestId, "/health", startedAtNanos, exception);
        } catch (RestClientException exception) {
            throw invalidResponseException(requestId, "/health", startedAtNanos, exception);
        }
    }

    private void requireConfigured() {
        if (restClient == null) {
            throw new PythonBackendClientException(
                    FailureType.CONNECTION_FAILED,
                    "Python backend client is not configured"
            );
        }
    }

    private PythonBackendClientException responseException(
            String requestId,
            String path,
            long startedAtNanos,
            RestClientResponseException exception
    ) {
        FailureType failureType = classifyHttpFailure(exception.getStatusCode());
        String backendErrorCode = extractFaceEmbeddingErrorCode(exception);
        log.warn("Python backend request failed: requestId={}, path={}, durationMs={}, status={}, failureType={}, backendErrorCode={}",
                requestId, path, elapsedMillis(startedAtNanos), exception.getStatusCode(), failureType, backendErrorCode);
        return new PythonBackendClientException(failureType, messageFor(failureType), backendErrorCode, exception);
    }

    private String extractFaceEmbeddingErrorCode(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }
        try {
            return findFaceEmbeddingErrorCode(objectMapper.readTree(responseBody));
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private static String findFaceEmbeddingErrorCode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> field = fields.next();
                if (BUSINESS_CODE_FIELD_NAMES.contains(field.getKey()) && field.getValue().isTextual()) {
                    String directMatch = normalizeFaceEmbeddingErrorCode(field.getValue().asText());
                    if (directMatch != null) {
                        return directMatch;
                    }
                }
                String nestedMatch = findFaceEmbeddingErrorCode(field.getValue());
                if (nestedMatch != null) {
                    return nestedMatch;
                }
            }
            return null;
        }
        if (node.isArray()) {
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                String errorCode = findFaceEmbeddingErrorCode(children.next());
                if (errorCode != null) {
                    return errorCode;
                }
            }
        }
        return null;
    }

    private static String normalizeFaceEmbeddingErrorCode(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String normalized = candidate.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return FACE_EMBEDDING_ERROR_CODES.contains(normalized) ? normalized : null;
    }

    private static boolean isValidEmbedding(FaceEmbeddingResponse response) {
        return response.dimension() != null
                && response.dimension() == 512
                && response.embedding() != null
                && response.embedding().size() == 512
                && response.embedding().stream().allMatch(value -> value != null && Float.isFinite(value));
    }

    private static PythonBackendClientException resourceAccessException(
            String requestId,
            String path,
            long startedAtNanos,
            ResourceAccessException exception
    ) {
        FailureType failureType = classifyResourceAccessFailure(exception);
        log.error("Python backend unavailable: requestId={}, path={}, durationMs={}, failureType={}",
                requestId, path, elapsedMillis(startedAtNanos), failureType, exception);
        return new PythonBackendClientException(failureType, messageFor(failureType), exception);
    }

    private static PythonBackendClientException invalidResponseException(
            String requestId,
            String path,
            long startedAtNanos,
            RestClientException exception
    ) {
        log.warn("Python backend invalid response: requestId={}, path={}, durationMs={}",
                requestId, path, elapsedMillis(startedAtNanos));
        return new PythonBackendClientException(
                FailureType.INVALID_RESPONSE,
                messageFor(FailureType.INVALID_RESPONSE),
                exception
        );
    }

    static FailureType classifyResourceAccessFailure(ResourceAccessException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof SocketTimeoutException timeoutException) {
            String message = timeoutException.getMessage();
            return message != null && message.toLowerCase(Locale.ROOT).contains("connect")
                    ? FailureType.CONNECT_TIMEOUT
                    : FailureType.READ_TIMEOUT;
        }
        if (cause instanceof ConnectException) {
            return FailureType.CONNECTION_FAILED;
        }
        return FailureType.CONNECTION_FAILED;
    }

    private static FailureType classifyHttpFailure(HttpStatusCode statusCode) {
        if (statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            return FailureType.AUTHENTICATION_FAILED;
        }
        if (statusCode.value() == HttpStatus.FORBIDDEN.value()) {
            return FailureType.ACCESS_DENIED;
        }
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return FailureType.ENDPOINT_NOT_FOUND;
        }
        if (statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return FailureType.RATE_LIMITED;
        }
        return statusCode.is5xxServerError() ? FailureType.UPSTREAM_ERROR : FailureType.REJECTED;
    }

    private static String messageFor(FailureType failureType) {
        return switch (failureType) {
            case AUTHENTICATION_FAILED -> "Python backend authentication failed";
            case ACCESS_DENIED -> "Python backend access denied";
            case ENDPOINT_NOT_FOUND -> "Python backend endpoint was not found";
            case RATE_LIMITED -> "Python backend rate limit exceeded";
            case UPSTREAM_ERROR, CONNECTION_FAILED -> "Python backend is unavailable";
            case CONNECT_TIMEOUT, READ_TIMEOUT -> "Python backend request timed out";
            case INVALID_RESPONSE -> "Invalid response from Python backend";
            case REJECTED -> "Python backend rejected the request";
        };
    }

    private static HttpEntity<Resource> createFilePart(MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveContentType(file.getContentType()));
        headers.setContentDispositionFormData("file", resolvedFileName(file));
        return new HttpEntity<>(file.getResource(), headers);
    }

    private static String resolvedFileName(MultipartFile file) {
        String fileName = StringUtils.getFilename(file.getOriginalFilename());
        return StringUtils.hasText(fileName) ? fileName : "face-image";
    }

    private static MediaType resolveContentType(String contentType) {
        try {
            return StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String stripTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    public record FaceEmbeddingResponse(
            boolean success,
            java.util.List<Float> embedding,
            Integer dimension,
            String model,
            String errorCode,
            String message
    ) {
    }

    public enum FailureType {
        AUTHENTICATION_FAILED,
        ACCESS_DENIED,
        ENDPOINT_NOT_FOUND,
        RATE_LIMITED,
        UPSTREAM_ERROR,
        CONNECT_TIMEOUT,
        READ_TIMEOUT,
        CONNECTION_FAILED,
        REJECTED,
        INVALID_RESPONSE;

        public boolean isUnavailable() {
            return switch (this) {
                case UPSTREAM_ERROR, CONNECT_TIMEOUT, READ_TIMEOUT, CONNECTION_FAILED -> true;
                default -> false;
            };
        }
    }

    public static class PythonBackendClientException extends RuntimeException {
        private final FailureType failureType;
        private final String backendErrorCode;

        public PythonBackendClientException(FailureType failureType, String message) {
            this(failureType, message, null, null);
        }

        public PythonBackendClientException(FailureType failureType, String message, Throwable cause) {
            this(failureType, message, null, cause);
        }

        public PythonBackendClientException(
                FailureType failureType,
                String message,
                String backendErrorCode,
                Throwable cause
        ) {
            super(message, cause);
            this.failureType = failureType;
            this.backendErrorCode = backendErrorCode;
        }

        public FailureType getFailureType() {
            return failureType;
        }

        public String getBackendErrorCode() {
            return backendErrorCode;
        }
    }
}
