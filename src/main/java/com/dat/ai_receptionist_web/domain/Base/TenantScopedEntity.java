package com.dat.ai_receptionist_web.domain.Base;

import com.dat.ai_receptionist_web.context.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * MappedSuperclass cho các thực thể thuộc phạm vi một Tenant.
 * Tự động gán tenantId từ TenantContext khi persist và chặn ghi chéo tenant.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantScopedEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    protected void assignAndVerifyTenantOnPersist() {
        UUID current = TenantContext.getTenantIdOrNull();
        if (this.tenantId == null) {
            this.tenantId = TenantContext.requireTenantId();
        } else if (current != null && !Objects.equals(current, this.tenantId)) {
            throw new IllegalStateException(
                    "Cross-tenant persist blocked: context=" + current + ", entity=" + this.tenantId);
        }
    }

    @PreUpdate
    protected void verifyTenantOnUpdate() {
        UUID current = TenantContext.getTenantIdOrNull();
        if (current != null && !Objects.equals(current, this.tenantId)) {
            throw new IllegalStateException(
                    "Cross-tenant update blocked: context=" + current + ", entity=" + this.tenantId);
        }
    }
}
