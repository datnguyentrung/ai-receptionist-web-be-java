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
            UUID courseStaffAssignmentId,
            UUID classSessionId,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            String note,
            AllowedActions allowedActions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
