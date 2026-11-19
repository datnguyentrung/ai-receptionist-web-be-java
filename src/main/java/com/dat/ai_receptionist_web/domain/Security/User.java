package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tài khoản xác thực dùng để đăng nhập vào hệ thống.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", schema = "security", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_phone_number", columnNames = "phone_number"))
public class User {
    /** Mã định danh duy nhất của tài khoản. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Số điện thoại dùng làm tên đăng nhập của tài khoản. */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** Mã băm mật khẩu của tài khoản. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Trạng thái hoạt động của tài khoản. */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus status;

    /** Phiên bản phân quyền dùng để vô hiệu hóa quyền đã lưu trước đó. */
    @Column(name = "authorization_version", nullable = false)
    private long authorizationVersion;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Thời điểm tài khoản đăng nhập thành công gần nhất. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
