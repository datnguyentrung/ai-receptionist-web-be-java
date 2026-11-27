package com.dat.ai_receptionist_web.domain.Security;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Liên kết giữa một tài khoản người dùng và một vai trò được gán trong phạm vi một Tenant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_role", schema = "security",
        indexes = @Index(name = "idx_user_role_role", columnList = "tenant_id,role_id"))
public class UserRole {
    /** Khóa tổng hợp gồm tenantId, mã tài khoản và mã vai trò. */
    @EmbeddedId
    private Key id;

    /** Tài khoản người dùng được gán vai trò. */
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Vai trò được gán cho tài khoản (cùng tenant). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false),
            @JoinColumn(name = "role_id", referencedColumnName = "role_id", insertable = false, updatable = false)
    })
    private Role role;

    public UserRole(Key id, User user) {
        this.id = id;
        this.user = user;
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

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(name = "role_id", length = 50, nullable = false)
        private String roleId;
    }
}
