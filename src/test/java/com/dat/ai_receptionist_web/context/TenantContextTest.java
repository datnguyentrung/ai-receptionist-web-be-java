package com.dat.ai_receptionist_web.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requireTenantIdFailsClosedWhenMissing() {
        TenantContext.clear();
        assertThatThrownBy(TenantContext::requireTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    void runWithTenantRestoresPreviousContext() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        TenantContext.setTenantId(tenantA);
        String result = TenantContext.runWithTenant(tenantB, () -> {
            assertThat(TenantContext.requireTenantId()).isEqualTo(tenantB);
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(TenantContext.requireTenantId()).isEqualTo(tenantA);
    }
}
