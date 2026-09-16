package com.dat.ai_receptionist_web.dto.Notification;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import java.util.Set;
import java.util.UUID;

public final class NotificationDTO {
    private NotificationDTO() {
    }

    public record CreateRequest(@NotNull String title, @NotNull String body, @NotNull NotificationType notificationType,
                                @NotNull String referenceType, @NotNull String referenceId, @NotNull String payload,
                                Set<UUID> recipientUserIds, Set<UUID> recipientPersonIds,
                                Set<@NotBlank String> recipientRoleCodes) {
        public NotificationType type() {
            return notificationType;
        }
    }

    public record UpdateRequest(@NotNull String title, @NotNull String body, @NotNull NotificationType notificationType, @NotNull String referenceType, @NotNull String referenceId, @NotNull String payload) {
    }

    public record Response(UUID notificationId, String title, String body, NotificationType notificationType,
                           String referenceType, String referenceId, String payload, LocalDateTime createdAt,
                           Integer recipientCount) {
        public Response(UUID notificationId, String title, String body, NotificationType notificationType,
                        String referenceType, String referenceId, String payload, LocalDateTime createdAt) {
            this(notificationId, title, body, notificationType, referenceType, referenceId, payload, createdAt, null);
        }

        public Response(UUID notificationId, int recipientCount) {
            this(notificationId, null, null, null, null, null, null, null, recipientCount);
        }
    }

    public record SimpleResponse(UUID notificationId, String title, NotificationType notificationType,
                                 String referenceType, String referenceId, LocalDateTime createdAt,
                                 Integer recipientCount) {
    }
}
