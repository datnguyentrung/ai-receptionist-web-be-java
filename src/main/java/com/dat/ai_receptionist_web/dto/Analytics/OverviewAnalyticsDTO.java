package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: OverviewAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class OverviewAnalyticsDTO {

    private OverviewAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<UUID> trainingPlanIds,
            List<String> levels,
            List<String> genders,
            Integer minAge,
            Integer maxAge,
            List<String> belts
    ) {
    }

    public record Response(
            Summary summary,
            List<MonthlyTrend> monthlyTrend,
            List<BranchMatrixItem> branchMatrix,
            List<ClassAlert> classAlerts,
            DataQuality dataQuality
    ) {
    }

    public record Summary(
            Integer activeStudents,
            Integer newStudents,
            Integer reactivatedStudents,
            Integer inferredLeavers,
            Integer netMovement,
            BigDecimal retentionRate,
            BigDecimal attendanceRate,
            Integer atRiskStudents,
            Integer renewalsDue,
            BigDecimal averageFillRate,
            BigDecimal approvedRevenue
    ) {
    }

    public record MonthlyTrend(
            YearMonth month,
            Integer newStudents,
            Integer reactivatedStudents,
            Integer inferredLeavers,
            Integer netMovement,
            Integer activeStudents
    ) {
    }

    public record BranchMatrixItem(
            Long branchId,
            String branchName,
            Integer activeStudents,
            Integer newStudents,
            Integer inferredLeavers,
            BigDecimal attendanceRate,
            Integer riskCount
    ) {
    }

    public record ClassAlert(
            UUID courseScheduleId,
            String courseName,
            String branchName,
            Integer activeRoster,
            Integer inferredLeavers,
            BigDecimal attendanceRate,
            List<String> warningCodes
    ) {
    }

    public record DataQuality(
            Integer excludedMigrationRecords,
            Integer missingLeaveDates,
            Integer provisionalEnrollmentCount
    ) {
    }

    public record MovementDetailRequest(
            String metric,
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            Integer page,
            Integer size,
            String sort
    ) {
    }

    public record MovementDetailResponse(
            UUID personId,
            String personCode,
            String fullName,
            String personStatus,
            String eventType,
            LocalDate eventDate,
            Long branchId,
            String branchName,
            UUID courseScheduleId,
            String courseName,
            LocalDate inferredLeaveDate,
            String leaveDateQuality
    ) {
    }
}
