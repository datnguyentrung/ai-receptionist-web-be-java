package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonAvatarUrlCacheRebuildService {
    private static final int PAGE_SIZE = 500;

    private final PersonRepository personRepository;
    private final PersonAvatarUrlCacheService avatarUrlCacheService;

    public RebuildResult rebuild() {
        String generation = avatarUrlCacheService.startRebuild();
        int pageNumber = 0;
        int scanned = 0;
        int cached = 0;
        log.info("PERSON_AVATAR_CACHE_REBUILD_STARTED generation={}", generation);
        try {
            Page<PersonRepository.PersonAvatarProjection> page;
            do {
                page = personRepository.findPersonAvatarProjections(PageRequest.of(pageNumber++, PAGE_SIZE));
                Map<UUID, String> faceImagePaths = new HashMap<>();
                for (PersonRepository.PersonAvatarProjection person : page.getContent()) {
                    scanned++;
                    faceImagePaths.put(person.getPersonId(), person.getFaceImagePath());
                    if (person.getFaceImagePath() != null && !person.getFaceImagePath().isBlank()) {
                        cached++;
                    }
                }
                avatarUrlCacheService.appendRebuildBatch(generation, faceImagePaths);
            } while (page.hasNext());
            avatarUrlCacheService.completeRebuild(generation);
            log.info("PERSON_AVATAR_CACHE_REBUILD_COMPLETED generation={} scanned={} cached={}",
                    generation, scanned, cached);
            return new RebuildResult(generation, scanned, cached);
        } catch (RuntimeException exception) {
            avatarUrlCacheService.abortRebuild(generation);
            log.error("PERSON_AVATAR_CACHE_REBUILD_FAILED generation={}", generation, exception);
            throw exception;
        }
    }

    public record RebuildResult(String generation, int scanned, int cached) {
    }
}
