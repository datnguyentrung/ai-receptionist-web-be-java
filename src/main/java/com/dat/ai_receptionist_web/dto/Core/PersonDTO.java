package com.dat.ai_receptionist_web.dto.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class PersonDTO {
    private PersonDTO() {
    }

    public record CreateRequest(
            @NotBlank
            String fullName,
            @NotNull
            Boolean gender,
            @NotNull
            LocalDate birthDate,
            String email,
            String nationalCode,
            String faceImagePath,
            @NotNull
            Belt currentBelt,
            @NotNull
            PersonStatus status,
            @NotNull
            LocalDate startDate,
            UUID positionId
    ) {
    }

    public record UpdateRequest(
            @NotNull
            String fullName,
            @NotNull
            Boolean gender,
            @NotNull
            LocalDate birthDate,
            String email,
            String nationalCode,
            String faceImagePath,
            String personCode,
            @NotNull
            Belt currentBelt,
            @NotNull
            PersonStatus status,
            @NotNull
            LocalDate startDate,
            UUID positionId
    ) {
    }

    public record Response(
            UUID personId,
            String fullName,
            Boolean gender,
            LocalDate birthDate,
            String email,
            String nationalCode,
            String personCode,
            Belt currentBelt,
            PersonStatus status,
            LocalDate startDate,
            PositionDTO.SimpleResponse position,
            String faceImagePath,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record BriefResponse(
            UUID personId,
            String fullName,
            String personCode,
            Belt currentBelt,
            PersonStatus status,
            String faceImagePath
    ) {
    }

    public record SimpleResponse(
            UUID personId,
            String fullName,
            Boolean gender,
            LocalDate birthDate,
            String personCode,
            Belt currentBelt,
            PersonStatus status,
            String faceImagePath
    ) {
    }

    public record FaceEmbeddingUpdateResponse(
            UUID personId,
            int dimension,
            String model,
            String faceImagePath,
            String avatarUrl,
            LocalDateTime updatedAt
    ) {
    }

    public record FaceImageUrlResponse(String url) {
    }
}
