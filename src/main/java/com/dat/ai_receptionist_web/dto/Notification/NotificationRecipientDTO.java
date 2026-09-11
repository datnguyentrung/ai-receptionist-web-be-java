package com.dat.ai_receptionist_web.dto.Notification;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public final class NotificationRecipientDTO {
    private NotificationRecipientDTO() {
    }

    public record CreateRequest(@NotNull UUID notificationId, @NotNull UUID recipientUserId, UUID contextPersonId) {
    }

    public record UpdateRequest(boolean read, LocalDateTime deliveredAt, @NotNull NotificationRecipientStatus notificationRecipientStatus) {
    }

    public record Response(UUID notificationRecipientId, UUID notificationId, UUID recipientUserId,
                           UUID contextPersonId, boolean read, LocalDateTime readAt,
                           LocalDateTime deliveredAt, NotificationRecipientStatus notificationRecipientStatus,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record MineResponse(UUID notificationRecipientId, UUID notificationId, UUID contextPersonId,
                               String title, String body, NotificationType notificationType,
                               String referenceType, String referenceId, String payload,
                               boolean read, LocalDateTime readAt, LocalDateTime deliveredAt,
                               NotificationRecipientStatus notificationRecipientStatus,
                               LocalDateTime createdAt) {
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
