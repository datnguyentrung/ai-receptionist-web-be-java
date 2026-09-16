package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public final class AuthSessionDTO {
    private AuthSessionDTO() {
    }

    public record CreateRequest(@NotNull UUID userId, @NotNull UUID activeUserPersonId, @NotNull String refreshTokenHash, @NotNull String deviceInfo, @NotNull String platform, @NotNull String fcmToken, @NotNull LocalDateTime expiresAt, boolean revoked, @NotNull LocalDateTime revokedAt, long version) {
    }

    public record UpdateRequest(@NotNull UUID userId, @NotNull UUID activeUserPersonId, @NotNull String refreshTokenHash, @NotNull String deviceInfo, @NotNull String platform, @NotNull String fcmToken, @NotNull LocalDateTime expiresAt, boolean revoked, @NotNull LocalDateTime revokedAt, long version) {
    }

    public record Response(UUID authSessionId, UserDTO.SimpleResponse user, com.dat.ai_receptionist_web.dto.Core.UserPersonDTO.SimpleResponse activeUserPerson, String refreshTokenHash, String deviceInfo, String platform, String fcmToken, LocalDateTime expiresAt, boolean revoked, LocalDateTime revokedAt, long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record SimpleResponse(UUID authSessionId, UserDTO.SimpleResponse user, com.dat.ai_receptionist_web.dto.Core.UserPersonDTO.SimpleResponse activeUserPerson, String deviceInfo, String platform, LocalDateTime expiresAt, boolean revoked, LocalDateTime revokedAt, long version, LocalDateTime createdAt) {
    }
}
