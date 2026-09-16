package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;

public final class PermissionDTO {
    private PermissionDTO() {
    }

    public record CreateRequest(@NotNull String code, @NotNull String model, @NotNull PermissionAction action, @NotNull String description) {
    }

    public record UpdateRequest(@NotNull String code, @NotNull String model, @NotNull PermissionAction action, @NotNull String description) {
    }

    public record Response(Integer permissionId, String code, String model, PermissionAction action, String description) {
    }

    public record SimpleResponse(Integer permissionId, String code, String model, PermissionAction action) {
    }
}
