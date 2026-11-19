package com.dat.ai_receptionist_web.domain.Notification;

import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nội dung thông báo được gửi đến một hoặc nhiều người dùng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notification", schema = "notification")
public class Notification {
    /** Mã định danh duy nhất của thông báo. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    /** Tiêu đề của thông báo. */
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    /** Nội dung chính của thông báo. */
    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    /** Loại nghiệp vụ của thông báo. */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    /** Loại đối tượng nghiệp vụ được thông báo tham chiếu đến. */
    @Column(name = "reference_type", length = 100)
    private String referenceType;

    /** Mã định danh của đối tượng nghiệp vụ được thông báo tham chiếu đến. */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    /** Dữ liệu bổ sung của thông báo, thường được lưu dưới dạng JSON. */
    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    /** Thời điểm thông báo được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
