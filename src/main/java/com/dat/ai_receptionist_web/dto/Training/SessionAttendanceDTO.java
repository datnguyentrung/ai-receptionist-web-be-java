package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class SessionAttendanceDTO {
    private SessionAttendanceDTO() {
    }

    public record CreateRequest(
            @NotNull
            UUID classSessionId,
            UUID studentEnrollmentId,
            UUID courseStaffAssignmentId,
            @NotNull
            LocalDateTime checkInTime,
            @NotNull
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note
    ) {
        @AssertTrue(message = "Exactly one attendance participant is required")
        public boolean isExactlyOneParticipant() {
            return (studentEnrollmentId != null) ^ (courseStaffAssignmentId != null);
        }
    }

    public record UpdateRequest(
            @NotNull
            LocalDateTime checkInTime,
            @NotNull
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note
    ) {
    }

    public record BatchUpdateRequest(
            @NotEmpty
            List<BatchUpdateItem> records
    ) {
    }

    public record BatchUpdateItem(
            @NotNull
            UUID sessionAttendanceId,
            LocalDateTime checkInTime,
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note
    ) {
    }

    public record UpdateStatusRequest(
            @NotNull
            AttendanceStatus attendanceStatus,
            LocalDateTime checkInTime
    ) {
    }

    public record UpdateEvaluationRequest(
            EvaluationStatus evaluationStatus,
            String note
    ) {
    }

    public record ManualLogRequest(
            @NotNull
            UUID classSessionId,
            UUID studentEnrollmentId,
            UUID courseStaffAssignmentId,
            LocalDateTime checkInTime,
            @NotNull
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note
    ) {
        @AssertTrue(message = "Exactly one attendance participant is required")
        public boolean isExactlyOneParticipant() {
            return (studentEnrollmentId != null) ^ (courseStaffAssignmentId != null);
        }
    }

    public record QuickCheckInRequest(
            @NotNull
            UUID classSessionId,
            UUID studentEnrollmentId,
            UUID courseStaffAssignmentId
    ) {
        @AssertTrue(message = "Exactly one attendance participant is required")
        public boolean isExactlyOneParticipant() {
            return (studentEnrollmentId != null) ^ (courseStaffAssignmentId != null);
        }
    }

    public record BatchInitRequest(
            @NotNull
            UUID classSessionId
    ) {
    }

    public record BulkDeleteRequest(
            @NotEmpty
            List<UUID> sessionAttendanceIds
    ) {
    }

    public record AttendanceStats(
            long totalRecords,
            double attendanceRate,
            long presentCount,
            long absentCount,
            long excusedCount,
            long makeupCount,
            long lateCount,
            long evalGoodCount,
            long evalAverageCount,
            long evalWeakCount,
            long evalPendingCount
    ) {
    }

    public record AttendanceListResponse(
            AttendanceStats stats,
            com.dat.ai_receptionist_web.dto.PageResponse<SimpleResponse> attendances
    ) {
    }

    public record AllowedActions(
            boolean update,
            boolean delete
    ) {
        public static AllowedActions none() {
            return new AllowedActions(false, false);
        }
    }

    public record Response(
            UUID sessionAttendanceId,
            ClassSessionDTO.Response classSession,
            StudentEnrollmentDTO.Response studentEnrollment,
            CourseStaffAssignmentDTO.Response courseStaffAssignment,
            LocalDateTime checkInTime,
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note,
            AllowedActions allowedActions,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record BriefResponse(
            UUID sessionAttendanceId,
            ClassSessionBrief classSession,
            UUID studentEnrollmentId,
            UUID courseStaffAssignmentId,
            UUID participantPersonId,
            String participantName,
            LocalDateTime checkInTime,
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note,
            LocalDateTime createdAt
    ) {
    }

    public record ClassSessionBrief(
            UUID classSessionId,
            CourseDTO.BriefResponse course,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            SessionStatus status
    ) {
    }

    public record EvaluationUpdateResult(
            UUID sessionAttendanceId,
            UUID classSessionId,
            EvaluationStatus evaluationStatus,
            String note,
            LocalDateTime updatedAt
    ) {
    }

    public record SimpleResponse(
            UUID sessionAttendanceId,
            ClassSessionDTO.SimpleResponse classSession,
            StudentEnrollmentDTO.SimpleResponse studentEnrollment,
            CourseStaffAssignmentDTO.SimpleResponse courseStaffAssignment,
            LocalDateTime checkInTime,
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus,
            String note,
            LocalDateTime createdAt
    ) {
    }
}
