package com.dat.ai_receptionist_web.domain.Security;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * Liên kết giữa một vai trò và một quyền được cấp cho vai trò đó.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role_permission", schema = "security",
        indexes = @Index(name = "idx_role_permission_permission", columnList = "permission_id"))
public class RolePermission {
    /** Khóa tổng hợp gồm mã vai trò và mã quyền. */
    @EmbeddedId
    private Key id;

    /** Vai trò được cấp quyền. */
    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** Quyền được cấp cho vai trò. */
    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * Khóa tổng hợp của liên kết vai trò và quyền.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class Key implements Serializable {
        /** Mã của vai trò trong khóa tổng hợp. */
        @Column(name = "role_id", length = 50)
        private String roleId;

        /** Mã định danh của quyền trong khóa tổng hợp. */
        @Column(name = "permission_id")
        private Integer permissionId;
    }
}
