package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hồ sơ chung của một cá nhân trong hệ thống, có thể là học viên, huấn luyện viên hoặc nhân viên.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "person", schema = "core", uniqueConstraints = {
        @UniqueConstraint(name = "uk_person_national_code", columnNames = "national_code"),
        @UniqueConstraint(name = "uk_person_code", columnNames = "person_code")
}, indexes = @Index(name = "idx_person_position", columnList = "position_id"))
public class Person {
    /** Mã định danh duy nhất của cá nhân. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    /** Họ và tên đầy đủ của cá nhân. */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Giới tính của cá nhân theo quy ước của hệ thống. */
    @Column(name = "gender")
    private Boolean gender;

    /** Ngày sinh của cá nhân. */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** Cấp đai hiện tại của cá nhân. */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_belt", length = 20)
    private Belt currentBelt;

    /** Địa chỉ email liên hệ của cá nhân. */
    @Column(name = "email", length = 100)
    @Email
    private String email;

    /** Địa chỉ email khôi phục đã được xác minh. */
    @Column(name = "recovery_email_verified_value", length = 100)
    private String recoveryEmailVerifiedValue;

    /** Thời điểm email khôi phục được xác minh. */
    @Column(name = "recovery_email_verified_at")
    private LocalDateTime recoveryEmailVerifiedAt;

    /** Mã định danh công dân hoặc giấy tờ tùy thân của cá nhân. */
    @Column(name = "national_code", length = 50)
    private String nationalCode;

    /** Vectơ đặc trưng khuôn mặt dùng cho nhận diện và điểm danh. */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 512)
    @Column(name = "face_embedding", columnDefinition = "vector(512)")
    private float[] faceEmbedding;

    /** Đường dẫn lưu trữ ảnh khuôn mặt của cá nhân. */
    @Column(name = "face_image_path", length = 500)
    private String faceImagePath;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Mã nghiệp vụ duy nhất của cá nhân trong hệ thống. */
    @Column(name = "person_code", nullable = false, length = 50)
    private String personCode;

    /** Trạng thái hồ sơ của cá nhân. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersonStatus status;

    /** Ngày bắt đầu làm việc, học tập hoặc tham gia hệ thống. */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Chức vụ hoặc vị trí công việc của cá nhân. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;
}
