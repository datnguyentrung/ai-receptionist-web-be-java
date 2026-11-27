package com.dat.ai_receptionist_web.domain.Security;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Liên kết giữa một vai trò và một quyền được cấp cho vai trò đó trong phạm vi một Tenant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_permission", schema = "security",
        indexes = @Index(name = "idx_role_permission_permission", columnList = "permission_id"))
public class RolePermission {
    /** Khóa tổng hợp gồm tenantId, mã vai trò và mã quyền. */
    @EmbeddedId
    private Key id;

    /** Vai trò được cấp quyền (cùng tenant). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false),
            @JoinColumn(name = "role_id", referencedColumnName = "role_id", insertable = false, updatable = false)
    })
    private Role role;

    /** Quyền được cấp cho vai trò. */
    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    public RolePermission(Key id, Permission permission) {
        this.id = id;
        this.permission = permission;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "tenant_id", nullable = false)
        private UUID tenantId;

        @Column(name = "role_id", length = 50, nullable = false)
        private String roleId;

        @Column(name = "permission_id", nullable = false)
        private Integer permissionId;
    }
}
