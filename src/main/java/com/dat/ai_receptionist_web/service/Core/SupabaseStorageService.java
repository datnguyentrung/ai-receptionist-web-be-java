package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.config.Supabase.SupabaseProperties;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final ObjectProvider<RestClient> supabaseStorageRestClient;
    private final SupabaseProperties properties;

    public SupabaseStorageService(
            @Qualifier("supabaseStorageRestClient") ObjectProvider<RestClient> supabaseStorageRestClient,
            SupabaseProperties properties
    ) {
        this.supabaseStorageRestClient = supabaseStorageRestClient;
        this.properties = properties;
    }

    public ValidatedImage validateImage(MultipartFile file) {
        if (file == null) {
            throw new ApiException(CoreErrorCode.INVALID_IMAGE_FILE);
        }
        if (file.isEmpty()) {
            throw new ApiException(CoreErrorCode.EMPTY_IMAGE_FILE);
        }
        if (file.getSize() > properties.getStorage().getMaxFileSize()) {
            throw new ApiException(GeneralErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(CoreErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesSignature(contentType, bytes)) {
                throw new ApiException(CoreErrorCode.INVALID_IMAGE_FILE);
            }
            return new ValidatedImage(MediaType.parseMediaType(contentType), extensionFor(contentType), bytes);
        } catch (IOException exception) {
            throw new ApiException(CoreErrorCode.INVALID_IMAGE_FILE);
        }
    }

    public ValidatedImage validateTopUpProof(MultipartFile file) {
        ValidatedImage source = validateImage(file);
        if (!Set.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG).contains(source.contentType())) {
            throw new ApiException(CoreErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(source.bytes()));
            if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0
                    || (long) decoded.getWidth() * decoded.getHeight() > MAX_IMAGE_PIXELS) {
                throw new ApiException(CoreErrorCode.INVALID_IMAGE_FILE);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(decoded, source.extension(), output)) {
                throw new ApiException(CoreErrorCode.INVALID_IMAGE_FILE);
            }
            byte[] sanitized = output.toByteArray();
            if (sanitized.length == 0 || sanitized.length > properties.getStorage().getMaxFileSize()) {
                throw new ApiException(GeneralErrorCode.FILE_TOO_LARGE);
            }
            return new ValidatedImage(source.contentType(), source.extension(), sanitized);
        } catch (IOException exception) {
            throw new ApiException(CoreErrorCode.INVALID_IMAGE_FILE);
        }
    }

    public String uploadPersonFaceImage(UUID personId, MultipartFile file) {
        return uploadPersonFaceImage(personId, validateImage(file));
    }

    public String uploadPersonFaceImage(UUID personId, ValidatedImage image) {
        requireConfigured();
        String objectPath = "persons/%s/%s.%s".formatted(personId, UUID.randomUUID(), image.extension());
        String bucket = properties.getStorage().getFaceImageBucket();
        try {
            client().post()
                    .uri(uriBuilder -> storageObjectUri(uriBuilder, bucket, objectPath))
                    .header("x-upsert", "false")
                    .contentType(image.contentType())
                    .body(image.bytes())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Uploaded face image: personId={}, objectPath={}", personId, objectPath);
            return objectPath;
        } catch (RestClientResponseException exception) {
            logStorageResponseFailure("face-image upload", bucket, objectPath, exception);
            throw new ApiException(toStorageErrorCode(exception, CoreErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED));
        } catch (ResourceAccessException exception) {
            log.error("Supabase face-image upload unavailable: personId={}, objectPath={}",
                    personId, objectPath, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.error("Supabase face-image upload failed: personId={}, objectPath={}",
                    personId, objectPath, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED);
        }
    }

    public String uploadTopUpProof(UUID requestId, ValidatedImage image) {
        String objectPath = "requests/%s/%s.%s".formatted(requestId, UUID.randomUUID(), image.extension());
        return uploadPrivateImage(properties.getStorage().getTopUpProofBucket(), objectPath, image);
    }

    public void deleteTopUpProof(String objectPath) {
        deleteObject(properties.getStorage().getTopUpProofBucket(), objectPath);
    }

    public String createTopUpProofSignedUrl(String objectPath) {
        return createSignedUrl(properties.getStorage().getTopUpProofBucket(), objectPath);
    }

    public long topUpProofSignedUrlTtlSeconds() {
        return properties.getStorage().getSignedUrlTtlSeconds();
    }

    public void deleteObject(String objectPath) {
        deleteObject(properties.getStorage().getFaceImageBucket(), objectPath);
    }

    private void deleteObject(String bucket, String objectPath) {
        if (!StringUtils.hasText(objectPath)) {
            return;
        }
        requireConfigured(bucket);
        try {
            client().delete()
                    .uri(uriBuilder -> storageObjectUri(
                            uriBuilder,
                            bucket,
                            objectPath
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted storage object from bucket={}", bucket);
        } catch (RestClientResponseException exception) {
            logStorageResponseFailure("delete", bucket, objectPath, exception);
            throw new ApiException(toStorageErrorCode(exception, CoreErrorCode.SUPABASE_STORAGE_DELETE_FAILED));
        } catch (ResourceAccessException exception) {
            log.error("Supabase storage delete unavailable: bucket={}", bucket, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.error("Supabase storage delete failed: bucket={}", bucket, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_DELETE_FAILED);
        }
    }

    public String getPublicUrl(String objectPath) {
        if (!StringUtils.hasText(objectPath)) {
            return null;
        }
        requireConfigured();
        return stripTrailingSlash(properties.getUrl())
                + "/storage/v1/object/public/"
                + properties.getStorage().getFaceImageBucket()
                + "/"
                + objectPath;
    }

    public String createSignedUrl(String objectPath) {
        return createSignedUrl(properties.getStorage().getFaceImageBucket(), objectPath);
    }

    private String createSignedUrl(String bucket, String objectPath) {
        if (!StringUtils.hasText(objectPath)) {
            return null;
        }
        requireConfigured(bucket);
        try {
            SignedUrlResponse response = client().post()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("object", "sign", bucket)
                            .pathSegment(objectPath.split("/"))
                            .build())
                    .body(Map.of("expiresIn", properties.getStorage().getSignedUrlTtlSeconds()))
                    .retrieve()
                    .body(SignedUrlResponse.class);
            if (response == null || !StringUtils.hasText(response.signedURL())) {
                throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
            }
            return toAbsoluteStorageUrl(response.signedURL());
        } catch (RestClientResponseException exception) {
            logStorageResponseFailure("signed-url creation", bucket, objectPath, exception);
            throw new ApiException(toStorageErrorCode(exception, CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE));
        } catch (ResourceAccessException exception) {
            log.error("Supabase signed-url creation unavailable: bucket={}", bucket, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            log.error("Supabase signed-url creation failed: bucket={}", bucket, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
        }
    }

    private RestClient client() {
        RestClient client = supabaseStorageRestClient.getIfAvailable();
        if (client == null) {
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_NOT_CONFIGURED);
        }
        return client;
    }

    private void requireConfigured() {
        requireConfigured(properties.getStorage().getFaceImageBucket());
    }

    private void requireConfigured(String bucket) {
        if (!StringUtils.hasText(properties.getUrl())
                || !StringUtils.hasText(properties.getServiceRoleKey())
                || !StringUtils.hasText(bucket)) {
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_NOT_CONFIGURED);
        }
    }

    private String uploadPrivateImage(String bucket, String objectPath, ValidatedImage image) {
        requireConfigured(bucket);
        try {
            client().post()
                    .uri(uriBuilder -> storageObjectUri(uriBuilder, bucket, objectPath))
                    .header("x-upsert", "false")
                    .contentType(image.contentType())
                    .body(image.bytes())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Uploaded private image: bucket={}", bucket);
            return objectPath;
        } catch (RestClientResponseException exception) {
            logStorageResponseFailure("private-image upload", bucket, objectPath, exception);
            throw new ApiException(toStorageErrorCode(exception, CoreErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED));
        } catch (ResourceAccessException exception) {
            log.error("Supabase private-image upload unavailable: bucket={}", bucket, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException apiException) throw apiException;
            log.error("Supabase private-image upload failed: bucket={}", bucket, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UPLOAD_FAILED);
        }
    }

    private static CoreErrorCode toStorageErrorCode(RestClientResponseException exception, CoreErrorCode fallback) {
        return exception.getStatusCode().is5xxServerError()
                ? CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE
                : fallback;
    }

    private static void logStorageResponseFailure(
            String operation,
            String bucket,
            String objectPath,
            RestClientResponseException exception
    ) {
        log.error(
                "Supabase storage {} failed: bucket={}, objectPath={}, upstreamStatus={}, upstreamBody={}",
                operation,
                bucket,
                objectPath,
                exception.getStatusCode().value(),
                abbreviatedResponseBody(exception),
                exception);
    }

    private static String abbreviatedResponseBody(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(body)) return "<empty>";
        String normalized = body.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1_000 ? normalized : normalized.substring(0, 1_000) + "…";
    }

    private static boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case MediaType.IMAGE_PNG_VALUE -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case "image/webp" -> "webp";
            default -> throw new ApiException(CoreErrorCode.UNSUPPORTED_IMAGE_TYPE);
        };
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String toAbsoluteStorageUrl(String signedUrl) {
        if (signedUrl.startsWith("http://") || signedUrl.startsWith("https://")) {
            return signedUrl;
        }
        String normalizedPath = signedUrl.startsWith("/") ? signedUrl : "/" + signedUrl;
        return stripTrailingSlash(properties.getUrl()) + "/storage/v1" + normalizedPath;
    }

    private static java.net.URI storageObjectUri(
            org.springframework.web.util.UriBuilder uriBuilder,
            String bucket,
            String objectPath
    ) {
        return uriBuilder.pathSegment("object", bucket)
                .pathSegment(objectPath.split("/"))
                .build();
    }

    public record ValidatedImage(MediaType contentType, String extension, byte[] bytes) {
    }

    private record SignedUrlResponse(String signedURL) {
    }
}
