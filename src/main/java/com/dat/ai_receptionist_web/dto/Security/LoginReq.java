package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

public final class LoginReq {
    private LoginReq() {
    }

    @Data
    public static class UserBase {
        @NotBlank private String phoneNumber;
        @NotBlank private String password;
        @NotBlank private String idDevice;
        private String fcmToken;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class MobileLoginRequest extends UserBase {
        @NotBlank
        @Pattern(regexp = "(?i)^(ANDROID|IOS|WEB)$")
        private String platform;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank private String refreshToken;
    }

    @Data
    public static class UpdateFcmReq {
        @NotBlank private String fcmToken;
        @Pattern(regexp = "(?i)^(ANDROID|IOS|WEB)$")
        private String platform;
    }

    @Data
    public static class SwitchContextReq {
        @NotNull private UUID userPersonId;
    }

    @Data
    public static class SelectTenantReq {
        @NotNull private UUID tenantId;
    }
}
