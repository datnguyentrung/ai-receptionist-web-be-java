package com.dat.ai_receptionist_web.dto.Security;

import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import lombok.*;

import java.util.*;

@Data
@NoArgsConstructor
public class LoginRes {
    private String accessToken;
    private String idDevice;
    private UserLogin user;
    private TenantRes activeTenant;
    private List<TenantRes> availableTenants = List.of();
    private boolean requiresTenantSelection;
    private UserContextRes activeContext;
    private List<UserContextRes> availableContexts = List.of();
    private boolean requiresContextSelection;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MobileResponse {
        private String accessToken;
        private String refreshToken;
        private UserLogin user;
        private TenantRes activeTenant;
        private List<TenantRes> availableTenants;
        private boolean requiresTenantSelection;
        private UserContextRes activeContext;
        private List<UserContextRes> availableContexts;
        private boolean requiresContextSelection;
    }

    public record UserLogin(
            UUID userId,
            String phoneNumber,
            UserStatus status,
            Set<String> roles,
            Set<String> permissions
    ) {
    }

    public record TenantRes(
            UUID tenantId,
            String code,
            String name,
            TenantStatus status
    ) {
    }

    public record UserContextRes(
            UUID userPersonId,
            UUID personId,
            RelationshipType relationshipType,
            String personCode,
            String displayName,
            Boolean gender,
            String faceImagePath
    ) {
    }
}
