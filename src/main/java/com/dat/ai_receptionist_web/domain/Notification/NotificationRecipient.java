package com.dat.ai_receptionist_web.domain.Notification;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
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
@Table(name = "notification_recipient", schema = "notification", indexes = {
        @Index(name = "idx_notification_recipient_user", columnList = "recipient_user_id"),
        @Index(name = "idx_notification_recipient_inbox_context",
                columnList = "recipient_user_id,context_person_id,created_at DESC,notification_recipient_id DESC"),
        @Index(name = "idx_notification_recipient_unread_context",
                columnList = "recipient_user_id,context_person_id,created_at DESC,notification_recipient_id DESC")
})
public class NotificationRecipient {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "notification_recipient_id", nullable = false, updatable = false)
    private UUID notificationRecipientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "context_person_id")
    private Person contextPerson;

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_recipient_status", nullable = false, length = 30)
    private NotificationRecipientStatus notificationRecipientStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
