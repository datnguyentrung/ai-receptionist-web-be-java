package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Core.Tenant;
import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import com.dat.ai_receptionist_web.repository.Core.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Thực thi tác vụ nền/scheduler độc lập cho từng Tenant đang ACTIVE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantJobRunner {
    private final TenantRepository tenantRepository;

    public void runForAllActiveTenants(String jobName, Consumer<UUID> task) {
        List<Tenant> tenants = tenantRepository.findAllByStatusOrderByNameAsc(TenantStatus.ACTIVE);
        for (Tenant tenant : tenants) {
            UUID tenantId = tenant.getTenantId();
            try {
                TenantContext.runWithTenant(tenantId, () -> task.accept(tenantId));
            } catch (RuntimeException exception) {
                log.error("Tenant job failed job={} tenantId={} code={}",
                        jobName, tenantId, tenant.getCode(), exception);
            }
        }
    }
}
