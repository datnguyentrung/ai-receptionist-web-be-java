package com.dat.ai_receptionist_web.context;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * ThreadLocal giữ tenantId hiện hành cho HTTP request, worker, scheduler hoặc consumer.
 * Fail-closed: requireTenantId() ném IllegalStateException nếu thiếu tenant context.
 */
public final class TenantContext {
    public static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        if (tenantId == null) {
            CURRENT_TENANT.remove();
        } else {
            CURRENT_TENANT.set(tenantId);
        }
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static UUID getTenantIdOrNull() {
        return CURRENT_TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required but missing");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static <T> T runWithTenant(UUID tenantId, Supplier<T> action) {
        UUID previous = CURRENT_TENANT.get();
        try {
            setTenantId(tenantId);
            return action.get();
        } finally {
            setTenantId(previous);
        }
    }

    public static void runWithTenant(UUID tenantId, Runnable action) {
        UUID previous = CURRENT_TENANT.get();
        try {
            setTenantId(tenantId);
            action.run();
        } finally {
            setTenantId(previous);
        }
    }
}
