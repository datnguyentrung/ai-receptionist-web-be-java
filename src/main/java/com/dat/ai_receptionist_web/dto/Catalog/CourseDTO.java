package com.dat.ai_receptionist_web.dto.Catalog;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CourseDTO {
    private CourseDTO() {
    }

    public record CreateRequest(
            @NotNull(message = "At least one course schedule is required")
            List<CourseScheduleDTO.UpsertRequest> courseSchedules,

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
            @NotNull(message = "Status is required")
            CourseStatus status
    ) {
    }

    public record Response(
            UUID courseId,
            List<CourseScheduleDTO.Response> courseSchedules,
            String name,

            int capacity,
            int currentStudentCount,

            CourseStatus status,
            LocalDate classSessionGeneratedUntil,
            PersonDTO.BriefResponse primaryCoach,
            PersonDTO.BriefResponse manager,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID courseId,
            List<CourseScheduleDTO.SimpleResponse> courseSchedules,
            String name,
            int capacity,
            int currentStudentCount,
            CourseStatus status,
            PersonDTO.BriefResponse primaryCoach
    ) {
    }

    /** Internal compatibility type while the session planner is migrated. Not exposed by CourseController. */
    public record ScheduleChangeRequest(UUID classScheduleId, LocalDate effectiveFrom) {}
    public record CourseScheduleChangeResponse(Response course, List<UUID> cancelledSessionIds, List<UUID> generatedSessionIds) {}

}
