package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: AttendanceRiskAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class AttendanceRiskAnalyticsDTO {

    private AttendanceRiskAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseScheduleIds,
            List<UUID> courseIds,
            Integer minAge,
            Integer maxAge,
            List<String> riskLevels
    ) {
    }

    public record Response(
            Summary summary,
            List<ClassRow> byClass,
            List<WeeklyTrend> weeklyTrend,
            List<RiskReason> riskReasons,
            Thresholds thresholds
    ) {
    }

    public record Summary(
            Integer expectedAttendances,
            Integer presentCount,
            Integer lateCount,
            Integer makeupCount,
            Integer absentCount,
            Integer excusedCount,
            BigDecimal attendanceRate,
            Integer highRiskStudents,
            Integer mediumRiskStudents
    ) {
    }

    public record ClassRow(
            UUID courseScheduleId,
            String courseName,
            Integer expectedAttendances,
            BigDecimal attendanceRate,
            Integer highRiskCount
    ) {
    }

    public record WeeklyTrend(
            LocalDate weekStart,
            BigDecimal attendanceRate,
            Integer absentCount,
            Integer excusedCount
    ) {
    }

    public record RiskReason(
            String reasonCode,
            Integer studentCount
    ) {
    }

    public record Thresholds(
            Integer consecutiveAbsenceLimit,
            Integer daysWithoutAttendanceLimit,
            BigDecimal attendanceDropThreshold
    ) {
    }

    public record StudentsRequest(
            LocalDate from,
            LocalDate to,
            Long branchId,
            UUID courseScheduleId,
            String riskLevel,
            String riskReason,
            Integer page,
            Integer size
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            String personStatus,
            Long branchId,
            UUID courseScheduleId,
            LocalDate lastAttendanceDate,
            Integer daysSinceLastAttendance,
            BigDecimal attendanceRate30d,
            BigDecimal attendanceRatePrevious30d,
            Integer consecutiveAbsences,
            Integer excusedAbsences30d,
            String riskLevel,
            List<String> riskReasons
    ) {
    }
}
