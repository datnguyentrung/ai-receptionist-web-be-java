package com.dat.ai_receptionist_web.enums.Security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SystemRoleDefinition {

    SUPER_ADMIN(
            "SUPER_ADMIN",
            "Super Administrator",
            "Has all permissions in the system"
    ),

    SYSTEM_ADMIN(
            "SYSTEM_ADMIN",
            "System Administrator",
            "Has administrative privileges within the system"
    ),

    GUARDIAN(
            "GUARDIAN",
            "Guardian",
            "Parent or guardian with access to dependent student information"
    );

    private final String code;
    private final String name;
    private final String description;
}
