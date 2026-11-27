package com.dat.ai_receptionist_web.service.Training.session;

import com.dat.ai_receptionist_web.service.Core.TenantJobRunner;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter @Scheduled không chứa nghiệp vụ: chạy độc lập theo từng tenant đang ACTIVE.
 */
@Component
@RequiredArgsConstructor
@Order(10)
public class ClassSessionScheduler implements ApplicationRunner {
    private final TenantJobRunner tenantJobRunner;
    private final CourseSessionPlanningService planningService;
    private final ClassSessionLifecycleService lifecycleService;

    @Override
    public void run(ApplicationArguments args) {
        tenantJobRunner.runForAllActiveTenants("class-session-startup", tenantId -> {
            planningService.maintainGenerationHorizon();
            lifecycleService.activateSessions();
            lifecycleService.closeDueSessions();
            lifecycleService.completeSessions();
        });
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void maintainGenerationHorizon() {
        tenantJobRunner.runForAllActiveTenants("maintain-generation-horizon",
                tenantId -> planningService.maintainGenerationHorizon());
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void activateSessions() {
        tenantJobRunner.runForAllActiveTenants("activate-sessions",
                tenantId -> lifecycleService.activateSessions());
    }

    @Scheduled(cron = "40 */10 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void closeAttendance() {
        tenantJobRunner.runForAllActiveTenants("close-due-sessions",
                tenantId -> lifecycleService.closeDueSessions());
    }

    @Scheduled(cron = "20 */10 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void completeSessions() {
        tenantJobRunner.runForAllActiveTenants("complete-sessions",
                tenantId -> lifecycleService.completeSessions());
    }
}
