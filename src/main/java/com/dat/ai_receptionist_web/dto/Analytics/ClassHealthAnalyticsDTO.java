package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: ClassHealthAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class ClassHealthAnalyticsDTO {

    private ClassHealthAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<String> levels,
            List<String> scheduleStatuses,
            String sortBy,
            String sortDirection
    ) {
    }

    public record Response(
            Summary summary,
            List<ClassRow> classes,
            List<MonthlyTrend> monthlyTrend
    ) {
    }

    public record Summary(
            Integer totalClasses,
            Integer classesWithDecline,
            Integer classesAtRisk,
            Integer totalActiveRoster,
            BigDecimal averageAttendanceRate
    ) {
    }

    public record ClassRow(
            UUID courseScheduleId,
            UUID courseId,
            String courseName,
            UUID classScheduleId,
            Long branchId,
            String branchName,
            String weekday,
            LocalTime startTime,
            LocalTime endTime,
            String scheduleStatus,
            Integer capacity,
            String capacityBasis,
            Integer activeRoster,
            Integer newStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut,
            Integer netMovement,
            BigDecimal attendanceRate,
            BigDecimal fillRate,
            Integer atRiskStudents,
            List<String> warningCodes
    ) {
    }

    public record MonthlyTrend(
            YearMonth month,
            UUID courseScheduleId,
            Integer activeRoster,
            Integer newStudents,
            Integer inferredLeavers,
            BigDecimal attendanceRate
    ) {
    }

    public record DetailRequest(
            LocalDate from,
            LocalDate to,
            LocalDate asOf
    ) {
    }

    public record DetailResponse(
            DetailClassInfo classInfo,
            DetailSummary summary,
            List<DetailMonthlyTrend> monthlyTrend,
            List<DetailRiskDistribution> riskDistribution,
            List<DetailStaffAssignment> staffAssignments
    ) {
    }

    public record DetailClassInfo(
            UUID courseScheduleId,
            UUID courseId,
            String courseName,
            UUID classScheduleId,
            Long branchId,
            String branchName,
            String weekday,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            String sessionType
    ) {
    }

    public record DetailSummary(
            Integer activeRoster,
            Integer newStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut,
            BigDecimal attendanceRate,
            Integer atRiskStudents,
            Integer capacity,
            BigDecimal fillRate
    ) {
    }

    public record DetailMonthlyTrend(
            YearMonth month,
            Integer activeRoster,
            Integer newStudents,
            Integer inferredLeavers,
            BigDecimal attendanceRate
    ) {
    }

    public record DetailRiskDistribution(
            String riskLevel,
            Integer studentCount
    ) {
    }

    public record DetailStaffAssignment(
            UUID staffPersonId,
            String assignmentType,
            String status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
    }

    public record StudentsRequest(
            String segment,
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            Integer page,
            Integer size
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            Integer age,
            String gender,
            String currentBelt,
            String personStatus,
            UUID enrollmentId,
            LocalDate enrollmentStartDate,
            LocalDate enrollmentEndDate,
            LocalDate scheduleEffectiveFrom,
            LocalDate scheduleEffectiveTo,
            LocalDate inferredLeaveDate,
            String leaveDateQuality,
            BigDecimal attendanceRate30d,
            Integer consecutiveAbsences,
            String riskLevel
    ) {
    }
}
