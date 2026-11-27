package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.context.TenantContext;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Vai trò dùng để gom nhóm các quyền được gán cho người dùng trong phạm vi một Tenant.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(Role.Key.class)
@Table(name = "role", schema = "security")
public class Role {
    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Mã duy nhất trong tenant và là thành phần khóa chính của vai trò. */
    @Id
    @Column(name = "role_id", nullable = false, length = 50)
    private String code;

    /** Tên hiển thị của vai trò. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Mô tả mục đích và phạm vi của vai trò. */
    @Column(name = "description", length = 255)
    private String description;

    /** Phiên bản quyền dùng để làm mới thông tin phân quyền đã lưu. */
    @Column(name = "permission_version", nullable = false)
    @Builder.Default
    private long permissionVersion = 1;

    @PrePersist
    protected void assignTenantOnPersist() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.requireTenantId();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID tenantId;
        private String code;
    }
}
