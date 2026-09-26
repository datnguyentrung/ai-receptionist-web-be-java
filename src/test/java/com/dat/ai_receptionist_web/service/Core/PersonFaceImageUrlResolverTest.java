package com.dat.ai_receptionist_web.service.Core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PersonFaceImageUrlResolverTest {
    private final PersonAvatarUrlCacheService cacheService = mock(PersonAvatarUrlCacheService.class);
    private final SupabaseStorageService storageService = mock(SupabaseStorageService.class);
    private final PersonFaceImageUrlResolver resolver = new PersonFaceImageUrlResolver(cacheService, storageService);

    @Test
    void returnsCachedSignedUrlWithoutCallingStorage() {
        UUID personId = UUID.randomUUID();
        when(cacheService.getMany(List.of(personId))).thenReturn(Map.of(personId, "https://signed.example/avatar"));

        String result = resolver.resolve(personId, "persons/1/avatar.jpg");

        assertThat(result).isEqualTo("https://signed.example/avatar");
        verifyNoInteractions(storageService);
    }

    @Test
    void createsAndCachesSignedUrlWhenCacheMisses() {
        UUID personId = UUID.randomUUID();
        when(cacheService.getMany(List.of(personId))).thenReturn(Map.of());
        when(storageService.createSignedUrl("persons/1/avatar.jpg")).thenReturn("https://signed.example/avatar");

        String result = resolver.resolve(personId, "persons/1/avatar.jpg");

        assertThat(result).isEqualTo("https://signed.example/avatar");
        verify(cacheService).put(personId, "https://signed.example/avatar");
    }

    @Test
    void blankPathReturnsNull() {
        assertThat(resolver.resolve(UUID.randomUUID(), " ")).isNull();
        verifyNoInteractions(cacheService, storageService);
    }
}
