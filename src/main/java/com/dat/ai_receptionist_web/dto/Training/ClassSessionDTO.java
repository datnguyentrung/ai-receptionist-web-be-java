package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public final class ClassSessionDTO {
    private ClassSessionDTO() {
    }

    public record CreateRequest(
            @NotNull
            UUID courseId,
            @NotNull
            LocalDate sessionDate,
            @NotNull
            SessionStatus status,
            @NotNull
            LocalTime startTime,
            @NotNull
            LocalTime endTime,
            @NotNull
            String note
    ) {
    }

    public record UpdateRequest(
            @NotNull
            UUID courseId,
            @NotNull
            LocalDate sessionDate,
            @NotNull
            SessionStatus status,
            @NotNull
            LocalTime startTime,
            @NotNull
            LocalTime endTime,
            @NotNull
            String note
    ) {
    }

    public record Response(
            UUID classSessionId,
            CourseDTO.Response course,
            LocalDate sessionDate,
            SessionStatus status,
            boolean attendanceClosed,
            LocalDateTime attendanceReopenedUntil,
            LocalTime startTime,
            LocalTime endTime,
            String note
    ) {
    }

    public record SimpleResponse(
            UUID classSessionId,
            CourseDTO.SimpleResponse course,
            LocalDate sessionDate,
            SessionStatus status,
            boolean attendanceClosed,
            LocalTime startTime,
            LocalTime endTime,
            PersonDTO.SimpleResponse primaryCoach
    ) {
    }

    public record CalendarResponse(
            UUID classSessionId,
            UUID courseId,
            String courseName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            SessionStatus status,
            boolean attendanceClosed,
            PersonDTO.SimpleResponse primaryCoach
    ) {
    }

    public record ReopenAttendanceRequest(
            @NotNull
            LocalDateTime attendanceReopenedUntil
    ) {
    }
}
