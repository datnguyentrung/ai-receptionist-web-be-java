package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public final class StudentEnrollmentDTO {
    private StudentEnrollmentDTO() {
    }

    public record CreateRequest(
            @NotNull
            UUID studentPersonId,
            @NotNull
            UUID coursePurchaseId,
            @NotNull
            @NotNull List<UUID> courseScheduleIds,
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
            @NotNull List<UUID> courseScheduleIds,
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
            PersonDTO.Response studentPerson,
            UUID coursePurchaseId,
            List<CourseScheduleDTO.Response> courseSchedules,
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
            List<CourseScheduleDTO.SimpleResponse> courseSchedules,
            LocalDate startDate,
            LocalDate endDate,
            StudentEnrollmentStatus status
    ) {
    }
}
