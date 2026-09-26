package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.config.Supabase.SupabaseProperties;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
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

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class SupabaseStorageService {
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
            throw new ApiException(CoreErrorCode.FILE_TOO_LARGE);
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
            log.error("Supabase face-image upload failed: personId={}, objectPath={}, status={}",
                    personId, objectPath, exception.getStatusCode().value(), exception);
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

    public void deleteObject(String objectPath) {
        if (!StringUtils.hasText(objectPath)) {
            return;
        }
        requireConfigured();
        try {
            client().delete()
                    .uri(uriBuilder -> storageObjectUri(
                            uriBuilder,
                            properties.getStorage().getFaceImageBucket(),
                            objectPath
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted face image: objectPath={}", objectPath);
        } catch (RestClientResponseException exception) {
            log.error("Supabase face-image delete failed: objectPath={}, status={}",
                    objectPath, exception.getStatusCode().value(), exception);
            throw new ApiException(toStorageErrorCode(exception, CoreErrorCode.SUPABASE_STORAGE_DELETE_FAILED));
        } catch (ResourceAccessException exception) {
            log.error("Supabase face-image delete unavailable: objectPath={}", objectPath, exception);
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.error("Supabase face-image delete failed: objectPath={}", objectPath, exception);
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

    private RestClient client() {
        RestClient client = supabaseStorageRestClient.getIfAvailable();
        if (client == null) {
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_NOT_CONFIGURED);
        }
        return client;
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(properties.getUrl())
                || !StringUtils.hasText(properties.getServiceRoleKey())
                || !StringUtils.hasText(properties.getStorage().getFaceImageBucket())) {
            throw new ApiException(CoreErrorCode.SUPABASE_STORAGE_NOT_CONFIGURED);
        }
    }

    private static CoreErrorCode toStorageErrorCode(RestClientResponseException exception, CoreErrorCode fallback) {
        return exception.getStatusCode().is5xxServerError()
                ? CoreErrorCode.SUPABASE_STORAGE_UNAVAILABLE
                : fallback;
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
}
