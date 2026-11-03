package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PasswordRecoveryReq {
    private PasswordRecoveryReq() {}

    public enum Channel { APP, EMAIL }

    public record Start(@NotBlank String phoneNumber) {}
    public record EmailOption(UUID userPersonId, String maskedEmail) {}
    public record Options(UUID requestId, boolean appAvailable, List<EmailOption> emails) {}
    public record Send(@NotNull UUID requestId, @NotNull Channel channel, UUID userPersonId) {}
    public record Verify(@NotNull UUID requestId, @NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}
    public record Complete(@NotBlank String resetToken, @NotBlank @Size(min = 6) String newPassword,
                           @NotBlank String confirmPassword) {}
    public record EmailVerification(UUID userPersonId, String maskedEmail, boolean verified) {}
    public record EmailVerificationCode(@NotBlank @Pattern(regexp = "^\\d{6}$") String code) {}
    public record Sent(Instant resendAvailableAt, Instant expiresAt) {}
    public record Verified(String resetToken, Instant expiresAt) {}
}
