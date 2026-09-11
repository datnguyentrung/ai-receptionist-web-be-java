package com.dat.ai_receptionist_web.service.Security.access;

import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;

import java.util.Set;
import java.util.UUID;

public record AccessContext(
        UUID userId,
        UUID activeUserPersonId,
        UUID activePersonId,
        RelationshipType relationshipType,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {
    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCodes.contains(permissionCode);
    }

    public boolean hasUnrestrictedRole() {
        return hasRole(SystemRoleDefinition.SUPER_ADMIN.getCode()) || hasRole(SystemRoleDefinition.SYSTEM_ADMIN.getCode());
    }
}
