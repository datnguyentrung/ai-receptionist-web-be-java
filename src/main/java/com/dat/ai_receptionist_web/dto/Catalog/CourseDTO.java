package com.dat.ai_receptionist_web.dto.Catalog;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import java.util.UUID;

public final class CourseDTO {
    private CourseDTO() {
    }

    public record CreateRequest(
            @NotNull(message = "Class schedule ID is required")
            UUID classScheduleId,

            @NotBlank(message = "Name is required")
            String name,

            int capacity,

            @NotNull(message = "Status is required")
            CourseStatus status
    ) {
    }

    public record UpdateRequest(
            @NotNull(message = "Name is required")
            String name,

            int capacity,
            @NotNull(message = "Status is required") CourseStatus status) {
    }

    public record Response(
            UUID courseId,
            ClassScheduleDTO.Response classSchedule,
            ClassScheduleDTO.SimpleResponse nextClassSchedule,
            LocalDate nextScheduleEffectiveFrom,
            String name,
            int capacity,
            CourseStatus status,
            LocalDate classSessionGeneratedUntil,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SimpleResponse(
            UUID courseId,
            ClassScheduleDTO.SimpleResponse classSchedule,
            ClassScheduleDTO.SimpleResponse nextClassSchedule,
            LocalDate nextScheduleEffectiveFrom,
            String name,
            int capacity,
            CourseStatus status) {
    }

    public record ScheduleChangeRequest(
            @NotNull(message = "Class schedule ID is required")
            UUID classScheduleId,

            @NotNull(message = "Effective from is required")
            LocalDate effectiveFrom) {
    }

    public record CourseScheduleChangeResponse(
            Response course,
            java.util.List<UUID> cancelledSessionIds,
            java.util.List<UUID> generatedSessionIds) {
    }
}
