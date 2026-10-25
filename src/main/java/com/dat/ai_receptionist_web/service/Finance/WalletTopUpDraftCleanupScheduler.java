package com.dat.ai_receptionist_web.service.Finance;

import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestStatus;
import com.dat.ai_receptionist_web.repository.Finance.WalletTopUpRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletTopUpDraftCleanupScheduler {
    private final WalletTopUpRequestRepository repository;
    private final WalletTopUpRequestService service;

    @Scheduled(cron = "0 20 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void purgeExpiredDrafts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        var expired = repository.findByStatusAndCreatedAtBefore(WalletTopUpRequestStatus.DRAFT, cutoff);
        for (var request : expired) {
            try {
                service.purgeExpiredDraft(request.getTopUpRequestId(), cutoff);
            } catch (RuntimeException exception) {
                log.error("Failed to purge expired top-up draft: requestId={}", request.getTopUpRequestId(), exception);
            }
        }
    }
}
