package com.dat.ai_receptionist_web.service.Core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private static final String AVATAR_URL_KEY = "person:avatar-url";

    private final StringRedisTemplate redisTemplate;
    private final SupabaseStorageService supabaseStorageService;

    public void putFromFaceImagePath(UUID personId, String faceImagePath) {
        if (personId == null) {
            return;
        }
        String avatarUrl = supabaseStorageService.getPublicUrl(faceImagePath);
        if (!StringUtils.hasText(avatarUrl)) {
            remove(personId);
            return;
        }
        put(personId, avatarUrl);
    }

    public void putFromFaceImagePaths(Map<UUID, String> faceImagePaths) {
        putFromFaceImagePaths(AVATAR_URL_KEY, faceImagePaths);
    }

    public String startRebuild() {
        return UUID.randomUUID().toString();
    }

    public void appendRebuildBatch(String generation, Map<UUID, String> faceImagePaths) {
        putFromFaceImagePaths(rebuildKey(generation), faceImagePaths);
    }

    public void completeRebuild(String generation) {
        String rebuildKey = rebuildKey(generation);
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(rebuildKey))) {
                redisTemplate.rename(rebuildKey, AVATAR_URL_KEY);
            } else {
                redisTemplate.delete(AVATAR_URL_KEY);
            }
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_REBUILD_COMPLETE_UNAVAILABLE generation={}", generation, exception);
            throw exception;
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_REBUILD_COMPLETE_FAILED generation={}", generation, exception);
            throw exception;
        }
    }

    public void abortRebuild(String generation) {
        try {
            redisTemplate.delete(rebuildKey(generation));
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_REBUILD_ABORT_UNAVAILABLE generation={}", generation, exception);
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_REBUILD_ABORT_FAILED generation={}", generation, exception);
        }
    }

    public void put(UUID personId, String avatarUrl) {
        if (personId == null) {
            return;
        }
        try {
            if (StringUtils.hasText(avatarUrl)) {
                redisTemplate.opsForHash().put(AVATAR_URL_KEY, personId.toString(), avatarUrl);
            } else {
                redisTemplate.opsForHash().delete(AVATAR_URL_KEY, personId.toString());
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
            redisTemplate.opsForHash().delete(AVATAR_URL_KEY, personId.toString());
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

        List<Object> fields = uniquePersonIds.stream()
                .map(UUID::toString)
                .map(Object.class::cast)
                .toList();
        try {
            List<Object> values = redisTemplate.opsForHash().multiGet(AVATAR_URL_KEY, fields);
            Map<UUID, String> avatarUrls = HashMap.newHashMap(uniquePersonIds.size());
            int index = 0;
            for (UUID personId : uniquePersonIds) {
                Object value = values == null ? null : values.get(index);
                if (value != null && StringUtils.hasText(value.toString())) {
                    avatarUrls.put(personId, value.toString());
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

    private void putFromFaceImagePaths(String key, Map<UUID, String> faceImagePaths) {
        if (faceImagePaths.isEmpty()) {
            return;
        }
        try {
            redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"rawtypes", "unchecked"})
                public Object execute(RedisOperations operations) {
                    for (Map.Entry<UUID, String> entry : faceImagePaths.entrySet()) {
                        UUID personId = entry.getKey();
                        if (personId == null) {
                            continue;
                        }
                        String avatarUrl = supabaseStorageService.getPublicUrl(entry.getValue());
                        if (StringUtils.hasText(avatarUrl)) {
                            operations.opsForHash().put(key, personId.toString(), avatarUrl);
                        } else {
                            operations.opsForHash().delete(key, personId.toString());
                        }
                    }
                    return null;
                }
            });
        } catch (RedisConnectionFailureException exception) {
            log.warn("PERSON_AVATAR_CACHE_BATCH_PUT_UNAVAILABLE count={}", faceImagePaths.size(), exception);
        } catch (DataAccessException exception) {
            log.warn("PERSON_AVATAR_CACHE_BATCH_PUT_FAILED count={}", faceImagePaths.size(), exception);
        }
    }

    private String rebuildKey(String generation) {
        return AVATAR_URL_KEY + ":rebuild:" + generation;
    }
}
