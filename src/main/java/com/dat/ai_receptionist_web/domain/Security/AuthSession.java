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

/**
 * Phiên đăng nhập của người dùng và thông tin dùng để cấp lại hoặc thu hồi token.
 */
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
    /** Mã định danh duy nhất của phiên đăng nhập. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "auth_session_id", nullable = false, updatable = false)
    private UUID authSessionId;

    /** Người dùng sở hữu phiên đăng nhập. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Mã băm của refresh token, không lưu token gốc. */
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    /** Thông tin thiết bị đã tạo phiên đăng nhập. */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    /** Nền tảng của thiết bị đã tạo phiên đăng nhập. */
    @Column(name = "platform", length = 20)
    private String platform;

    /** Token Firebase Cloud Messaging dùng để gửi thông báo đến thiết bị. */
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    /** Mã định danh tenant đang chọn trong phiên (null nếu đang ở trạng thái chờ chọn tenant). */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** Hồ sơ cá nhân đang được tài khoản lựa chọn trong phiên đăng nhập. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_user_person_id")
    private UserPerson activeUserPerson;

    /** Thời điểm refresh token và phiên đăng nhập hết hạn. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Cho biết phiên đăng nhập đã bị thu hồi hay chưa. */
    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /** Thời điểm phiên đăng nhập bị thu hồi. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Phiên bản dùng để kiểm soát cập nhật đồng thời của phiên đăng nhập. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
