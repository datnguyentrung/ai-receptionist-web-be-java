package com.dat.ai_receptionist_web.dto.Catalog;

import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class CourseScheduleDTO {
    private CourseScheduleDTO() {}

    public record UpsertRequest(
            @NotNull UUID classScheduleId,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            @NotNull ScheduleStatus status) {}

    public record Response(
            UUID courseScheduleId,
            ClassScheduleDTO.Response classSchedule,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record SimpleResponse(
            UUID courseScheduleId,
            ClassScheduleDTO.SimpleResponse classSchedule,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleStatus status) {}
}
