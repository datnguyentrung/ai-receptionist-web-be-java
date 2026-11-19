package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: CoachAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class CoachAnalyticsDTO {

    private CoachAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            List<UUID> courseScheduleIds,
            List<String> assignmentTypes,
            List<String> assignmentStatuses
    ) {
    }

    public record Response(
            Summary summary,
            List<CoachRow> coaches,
            List<AssignmentRow> assignments,
            List<MonthlyRow> monthly
    ) {
    }

    public record Summary(
            Integer assignedCoaches,
            Integer assignedSessions,
            Integer completedSessions,
            Integer missingTimesheets,
            BigDecimal timesheetCoverageRate
    ) {
    }

    public record CoachRow(
            UUID coachPersonId,
            String coachName,
            Integer assignedClassCount,
            Integer assignedSessionCount,
            Integer completedSessionCount,
            Integer timesheetCompletedCount,
            Integer missingTimesheetCount,
            BigDecimal averageClassAttendanceRate
    ) {
    }

    public record AssignmentRow(
            UUID coachPersonId,
            UUID courseScheduleId,
            String assignmentType,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status
    ) {
    }

    public record MonthlyRow(
            YearMonth month,
            UUID coachPersonId,
            Integer assignedSessions,
            Integer completedSessions,
            Integer missingTimesheets
    ) {
    }

    public record SessionsRequest(
            LocalDate from,
            LocalDate to,
            Long branchId,
            String sessionStatus,
            String timesheetStatus,
            Integer page,
            Integer size
    ) {
    }

    public record SessionsResponse(
            UUID sessionId,
            LocalDate sessionDate,
            UUID courseScheduleId,
            String courseName,
            Long branchId,
            String assignmentType,
            String sessionStatus,
            LocalTime scheduledStartTime,
            LocalTime scheduledEndTime,
            LocalDateTime coachCheckIn,
            LocalDateTime coachCheckOut,
            Boolean timesheetComplete,
            BigDecimal studentAttendanceRate
    ) {
    }
}
