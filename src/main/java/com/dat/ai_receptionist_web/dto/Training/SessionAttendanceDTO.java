package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public final class SessionAttendanceDTO {
    private SessionAttendanceDTO() {
    }

    public record CreateRequest(
            @NotNull UUID classSessionId,
            UUID studentEnrollmentId,
            UUID courseStaffAssignmentId,
            @NotNull LocalDateTime checkInTime,
            @NotNull AttendanceStatus attendanceStatus,
            @NotNull EvaluationStatus evaluationStatus,
            @NotNull String note
    ) {
        @AssertTrue(message = "Exactly one attendance participant is required")
        public boolean isExactlyOneParticipant() {
            return (studentEnrollmentId != null) ^ (courseStaffAssignmentId != null);
        }
    }

    public record UpdateRequest(
            @NotNull LocalDateTime checkInTime,
            @NotNull AttendanceStatus attendanceStatus,
            @NotNull EvaluationStatus evaluationStatus,
            @NotNull String note
    ) {
    }

    public record AllowedActions(boolean update, boolean delete) {
        public static AllowedActions none() {
            return new AllowedActions(false, false);
        }
    }

    public record Response(
            UUID sessionAttendanceId,
            UUID classSessionId,
            UUID studentEnrollmentId,
            UUID courseStaffAssignmentId,
            LocalDateTime checkInTime,
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note,
            AllowedActions allowedActions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
