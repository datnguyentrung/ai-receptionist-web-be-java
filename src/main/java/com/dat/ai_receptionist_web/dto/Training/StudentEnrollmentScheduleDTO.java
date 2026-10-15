package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class StudentEnrollmentScheduleDTO {
    private StudentEnrollmentScheduleDTO() {
    }

    public record UpsertRequest(@NotNull UUID courseScheduleId) {
    }

    public record CreateRequest(
            @NotNull UUID studentEnrollmentId,
            @NotNull UUID courseScheduleId) {
    }

    public record UpdateRequest(@NotNull UUID courseScheduleId) {
    }

    public record Response(
            UUID studentEnrollmentScheduleId,
            UUID studentEnrollmentId,
            CourseScheduleDTO.Response courseSchedule) {
    }

    public record SimpleResponse(
            UUID studentEnrollmentScheduleId,
            UUID studentEnrollmentId,
            CourseScheduleDTO.SimpleResponse courseSchedule) {
    }
}
