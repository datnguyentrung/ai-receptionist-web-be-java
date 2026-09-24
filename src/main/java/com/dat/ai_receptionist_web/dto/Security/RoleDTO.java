package com.dat.ai_receptionist_web.dto.Security;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class RoleDTO {
    private RoleDTO() {
    }

    public record CreateRequest(
            @NotNull
            String code,
            @NotNull
            String name,
            @NotNull
            String description,
            long permissionVersion
    ) {
    }

    public record UpdateRequest(
            @NotNull
            String name,
            @NotNull
            String description,
            long permissionVersion
    ) {
    }

    public record Response(
            String code,
            String name,
            String description,
            long permissionVersion,
            List<PermissionDTO.Response> permissions
    ) {
    }

    public record BriefResponse(
            String code,
            String name,
            long permissionVersion
    ) {
    }

    public record SimpleResponse(
            String code,
            String name,
            long permissionVersion,
            List<PermissionDTO.SimpleResponse> permissions
    ) {
    }
}
