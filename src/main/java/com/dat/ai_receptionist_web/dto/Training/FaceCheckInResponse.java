package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record FaceCheckInResponse(
        Status status,
        Action action,
        PersonSummary person,
        SessionSummary session,
        UUID recordId,
        LocalDateTime checkInTime,
        LocalTime checkOutTime,
        UUID requestId,
        float confidence,
        AttendanceStatus attendanceStatus,
        String message,
        ErrorSummary error
) {
    public enum Status {
        PENDING,
        PROCESSING,
        SUCCESS,
        REJECTED,
        FAILED,
        EXPIRED
    }

    public enum Action {
        STUDENT_CHECK_IN,
        STAFF_TIMESHEET_CHECKED_IN,
        STAFF_TIMESHEET_CHECKED_OUT,
        COACH_CHECKED_IN,
        COACH_CHECKED_OUT
    }

    public record PersonSummary(
            UUID personId,
            String fullName,
            String personCode,
            String faceImagePath
    ) {
    }

    public record SessionSummary(
            UUID classSessionId,
            String courseName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    public record ErrorSummary(
            String code,
            String title,
            String detail
    ) {
    }
}
