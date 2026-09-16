package com.dat.ai_receptionist_web.dto.Training;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class CoachTimesheetDTO {
    private CoachTimesheetDTO() {
    }

    public record CreateRequest(
            @NotNull UUID classSessionId,
            @NotNull LocalTime checkInTime,
            @NotNull LocalTime checkOutTime,
            @NotNull String note
    ) {
    }

    public record UpdateRequest(
            @NotNull LocalTime checkInTime,
            @NotNull LocalTime checkOutTime,
            @NotNull String note
    ) {
    }

    public record AllowedActions(boolean update, boolean delete) {
        public static AllowedActions none() {
            return new AllowedActions(false, false);
        }
    }

    public record Response(
            UUID coachTimesheetId,
            CourseStaffAssignmentDTO.Response courseStaffAssignment,
            ClassSessionDTO.Response classSession,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            String note,
            AllowedActions allowedActions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID coachTimesheetId,
            CourseStaffAssignmentDTO.SimpleResponse courseStaffAssignment,
            ClassSessionDTO.SimpleResponse classSession,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            String note,
            AllowedActions allowedActions
    ) {
    }
}
