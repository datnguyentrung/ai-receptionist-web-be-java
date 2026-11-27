package com.dat.ai_receptionist_web.service.Iot;

import com.dat.ai_receptionist_web.service.Core.TenantJobRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceAlertScheduler {
    private final TenantJobRunner tenantJobRunner;
    private final DeviceService devices;

    @Scheduled(fixedDelay = 30000)
    public void check() {
        tenantJobRunner.runForAllActiveTenants("device-alert-check", tenantId -> devices.checkOffline());
    }
}
