package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.enums.Security.UserStatus;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record AuthorizationSnapshot(
        UUID userId,
        String phoneNumber,
        UserStatus userStatus,
        long authorizationVersion,
        UUID tenantId,
        Set<String> roleCodes,
        Map<String, Long> rolePermissionVersions,
        Set<String> permissionCodes
) {
}
