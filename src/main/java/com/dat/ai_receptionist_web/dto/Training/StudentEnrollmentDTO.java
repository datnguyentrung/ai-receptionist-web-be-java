package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class StudentEnrollmentDTO {
    private StudentEnrollmentDTO() {
    }

    public record CreateRequest(
            @NotNull
            UUID studentPersonId,
            @NotNull
            UUID coursePurchaseId,
            @NotNull
            UUID classScheduleId,
            @NotNull
            LocalDate startDate,
            @NotNull
            LocalDate endDate,
            @NotNull
            StudentEnrollmentStatus status
    ) {
    }

    public record UpdateRequest(
            @NotNull
            UUID studentPersonId,
            @NotNull
            UUID coursePurchaseId,
            @NotNull
            UUID classScheduleId,
            @NotNull
            LocalDate startDate,
            @NotNull
            LocalDate endDate,
            @NotNull
            StudentEnrollmentStatus status
    ) {
    }

    public record Response(
            UUID studentEnrollmentId,
            PersonDTO.SimpleResponse studentPerson,
            UUID coursePurchaseId,
            UUID classScheduleId,
            LocalDate startDate,
            LocalDate endDate,
            StudentEnrollmentStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID studentEnrollmentId,
            PersonDTO.SimpleResponse studentPerson,
            UUID coursePurchaseId,
            UUID classScheduleId,
            LocalDate startDate,
            LocalDate endDate,
            StudentEnrollmentStatus status
    ) {
    }
}
