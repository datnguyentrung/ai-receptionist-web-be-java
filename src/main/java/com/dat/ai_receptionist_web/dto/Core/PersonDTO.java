package com.dat.ai_receptionist_web.dto.Core;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import java.time.LocalDate;
import java.util.UUID;

public final class PersonDTO {
    private PersonDTO() {
    }

    public record CreateRequest(
            @NotBlank String fullName,
            @NotNull Boolean gender,
            @NotNull LocalDate birthDate,
            String email,
            String nationalCode,
            String faceImagePath,
            @NotNull Belt currentBelt,
            @NotNull PersonStatus status,
            @NotNull LocalDate startDate,
            UUID positionId
    ) {
    }

    public record UpdateRequest(
            @NotNull String fullName,
            @NotNull Boolean gender,
            @NotNull LocalDate birthDate,
            String email,
            String nationalCode,
            String faceImagePath,
            String personCode,
            @NotNull Belt currentBelt,
            @NotNull PersonStatus status,
            @NotNull LocalDate startDate,
            UUID positionId) {
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
            UUID positionId,
            String faceImagePath,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
