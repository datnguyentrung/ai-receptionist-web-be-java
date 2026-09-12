package com.dat.ai_receptionist_web.dto.Core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public final class PositionDTO {
    private PositionDTO() {
    }

    public record CreateRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull Boolean active
    ) {
    }

    public record UpdateRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull Boolean active
    ) {
    }

    public record Response(
            UUID positionId,
            String code,
            String name,
            String description,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
