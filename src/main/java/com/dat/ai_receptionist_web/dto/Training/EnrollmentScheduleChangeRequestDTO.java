package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleChangeRequestStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;

public final class EnrollmentScheduleChangeRequestDTO {
    private EnrollmentScheduleChangeRequestDTO() {}

    public record CreateRequest(@NotNull UUID currentScheduleId, @NotNull UUID targetCourseScheduleId,
                                @NotNull LocalDate effectiveFrom, String note) {}
    public record ReviewRequest(String note) {}
    public record ScheduleOption(UUID courseScheduleId, UUID courseId, String courseName, String branchName,
                                 String weekday, LocalTime startTime, LocalTime endTime,
                                 SessionType sessionType, int remainingCapacity) {}
    public record Response(UUID requestId, UUID enrollmentId, UUID currentScheduleId, UUID targetCourseScheduleId,
                           LocalDate effectiveFrom, String note, EnrollmentScheduleChangeRequestStatus status,
                           UUID requestedByUserId, UUID reviewedByUserId, LocalDateTime reviewedAt, String reviewNote) {}
}
