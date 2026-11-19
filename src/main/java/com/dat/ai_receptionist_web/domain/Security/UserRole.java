package com.dat.ai_receptionist_web.domain.Security;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Liên kết giữa một tài khoản người dùng và một vai trò được gán.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_role", schema = "security",
        indexes = @Index(name = "idx_user_role_role", columnList = "role_id"))
public class UserRole {
    /** Khóa tổng hợp gồm mã tài khoản và mã vai trò. */
    @EmbeddedId
    private Key id;

    /** Tài khoản người dùng được gán vai trò. */
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Vai trò được gán cho tài khoản. */
    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Khóa tổng hợp của liên kết tài khoản và vai trò.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class Key implements Serializable {
        /** Mã định danh tài khoản trong khóa tổng hợp. */
        @Column(name = "user_id")
        private UUID userId;

        /** Mã vai trò trong khóa tổng hợp. */
        @Column(name = "role_id", length = 50)
        private String roleId;
    }
}
