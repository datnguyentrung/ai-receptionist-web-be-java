package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "auth_session", schema = "security", uniqueConstraints =
        @UniqueConstraint(name = "uk_auth_session_refresh_hash", columnNames = "refresh_token_hash"),
        indexes = {
                @Index(name = "idx_auth_session_user", columnList = "user_id"),
                @Index(name = "idx_auth_session_active_user_person_revoked",
                        columnList = "active_user_person_id,revoked")
        })
public class AuthSession {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "auth_session_id", nullable = false, updatable = false)
    private UUID authSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_user_person_id")
    private UserPerson activeUserPerson;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

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
