package com.dat.ai_receptionist_web.service.Sparring;

import com.dat.ai_receptionist_web.service.Core.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Fallback cho host quên đóng buổi: private video và state chỉ sống tối đa 24 giờ. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SparringPracticeVideoCleanupScheduler {
    private final SparringPracticeRoomRegistry rooms;
    private final SupabaseStorageService storage;

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanupAbandonedRooms() {
        long threshold = Instant.now().minus(Duration.ofHours(24)).toEpochMilli();
        for (String objectPath : rooms.removeAbandonedVideoPaths(threshold)) {
            try { storage.deletePracticeVideo(objectPath); }
            catch (RuntimeException error) { log.warn("Could not clean abandoned sparring-practice video: {}", objectPath, error); }
        }
    }
}
