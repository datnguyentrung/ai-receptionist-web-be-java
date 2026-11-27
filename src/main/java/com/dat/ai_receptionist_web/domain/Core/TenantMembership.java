package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Core.TenantMembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tư cách thành viên của một User toàn cục tại một Tenant cụ thể.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tenant_membership", schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_membership_tenant_user", columnNames = {"tenant_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_tenant_membership_user_status", columnList = "user_id,status")
        })
public class TenantMembership {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "tenant_membership_id", nullable = false, updatable = false)
    private UUID tenantMembershipId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TenantMembershipStatus status = TenantMembershipStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
