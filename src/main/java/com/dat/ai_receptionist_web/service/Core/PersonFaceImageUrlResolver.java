package com.dat.ai_receptionist_web.service.Core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonFaceImageUrlResolver {
    private final PersonAvatarUrlCacheService avatarUrlCacheService;
    private final SupabaseStorageService supabaseStorageService;

    public String resolve(UUID personId, String faceImagePath) {
        if (!StringUtils.hasText(faceImagePath)) {
            return null;
        }
        if (personId != null) {
            Map<UUID, String> cached = avatarUrlCacheService.getMany(List.of(personId));
            String cachedUrl = cached.get(personId);
            if (StringUtils.hasText(cachedUrl)) {
                return cachedUrl;
            }
        }
        String signedUrl = supabaseStorageService.createSignedUrl(faceImagePath);
        if (personId != null) {
            avatarUrlCacheService.put(personId, signedUrl);
        }
        return signedUrl;
    }
}
