package com.dat.ai_receptionist_web.service.Core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonAvatarUrlCacheService {
    private static final String AVATAR_URL_KEY_PREFIX = "person:avatar-url:";

    private final StringRedisTemplate redisTemplate;
    private final SupabaseStorageService supabaseStorageService;
    private final com.dat.ai_receptionist_web.config.Supabase.SupabaseProperties supabaseProperties;

    public void putFromFaceImagePath(UUID personId, String faceImagePath) {
        if (personId == null) {
            return;
        }
        String avatarUrl = supabaseStorageService.createSignedUrl(faceImagePath);
        if (!StringUtils.hasText(avatarUrl)) {
            remove(personId);
            return;
        }
        put(personId, avatarUrl);
    }

    public void putFromFaceImagePaths(Map<UUID, String> faceImagePaths) {
        putFromFaceImagePathsInternal(faceImagePaths);
    }

    public String startRebuild() {
        return UUID.randomUUID().toString();
    }

    public void appendRebuildBatch(String generation, Map<UUID, String> faceImagePaths) {
        putFromFaceImagePathsInternal(faceImagePaths);
    }

    public void completeRebuild(String generation) {
        // Signed URLs expire individually, so rebuild batches are written directly with per-key TTL.
    }

    public void abortRebuild(String generation) {
        // No staging key is used for signed URL rebuilds.
    }

    public void put(UUID personId, String avatarUrl) {
        if (personId == null) {
            return;
        }
        try {
            if (StringUtils.hasText(avatarUrl)) {
                redisTemplate.opsForValue().set(cacheKey(personId), avatarUrl, cacheTtl());
            } else {
                redisTemplate.delete(cacheKey(personId));
            }
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_PUT_UNAVAILABLE personId={}", personId, exception);
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_PUT_FAILED personId={}", personId, exception);
        }
    }

    public void remove(UUID personId) {
        if (personId == null) {
            return;
        }
        try {
            redisTemplate.delete(cacheKey(personId));
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_REMOVE_UNAVAILABLE personId={}", personId, exception);
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_REMOVE_FAILED personId={}", personId, exception);
        }
    }

    public Map<UUID, String> getMany(Collection<UUID> personIds) {
        Set<UUID> uniquePersonIds = new LinkedHashSet<>();
        for (UUID personId : personIds) {
            if (personId != null) {
                uniquePersonIds.add(personId);
            }
        }
        if (uniquePersonIds.isEmpty()) {
            return Map.of();
        }

        List<String> keys = uniquePersonIds.stream()
                .map(this::cacheKey)
                .toList();
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            Map<UUID, String> avatarUrls = HashMap.newHashMap(uniquePersonIds.size());
            int index = 0;
            for (UUID personId : uniquePersonIds) {
                String value = values == null ? null : values.get(index);
                if (StringUtils.hasText(value)) {
                    avatarUrls.put(personId, value);
                }
                index++;
            }
            return avatarUrls;
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_READ_UNAVAILABLE count={}", uniquePersonIds.size(), exception);
            return Map.of();
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_READ_FAILED count={}", uniquePersonIds.size(), exception);
            return Map.of();
        }
    }

    private void putFromFaceImagePathsInternal(Map<UUID, String> faceImagePaths) {
        if (faceImagePaths.isEmpty()) {
            return;
        }
        try {
            for (Map.Entry<UUID, String> entry : faceImagePaths.entrySet()) {
                UUID personId = entry.getKey();
                if (personId == null) {
                    continue;
                }
                String avatarUrl = supabaseStorageService.createSignedUrl(entry.getValue());
                if (StringUtils.hasText(avatarUrl)) {
                    redisTemplate.opsForValue().set(cacheKey(personId), avatarUrl, cacheTtl());
                } else {
                    redisTemplate.delete(cacheKey(personId));
                }
            }
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_BATCH_PUT_UNAVAILABLE count={}", faceImagePaths.size(), exception);
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_BATCH_PUT_FAILED count={}", faceImagePaths.size(), exception);
        }
    }

    private String cacheKey(UUID personId) {
        return AVATAR_URL_KEY_PREFIX + personId;
    }

    private Duration cacheTtl() {
        long signedTtlSeconds = Math.max(1L, supabaseProperties.getStorage().getSignedUrlTtlSeconds());
        long safetyWindow = Math.min(300L, Math.max(1L, signedTtlSeconds / 10L));
        return Duration.ofSeconds(Math.max(1L, signedTtlSeconds - safetyWindow));
    }
}
