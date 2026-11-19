package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO cho một page Analytics: RetentionAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class RetentionAnalyticsDTO {

    private RetentionAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<UUID> trainingPlanIds,
            List<String> genders,
            Integer minAge,
            Integer maxAge,
            Integer graceDays,
            String cohortGranularity
    ) {
    }

    public record Response(
            Summary summary,
            List<CohortRow> cohorts,
            List<TenureRow> byTenure,
            List<SegmentRow> bySegment,
            List<ExpiryRow> expiries,
            DataQuality quality
    ) {
    }

    public record Summary(
            Integer eligibleStudents,
            Integer inferredLeavers,
            BigDecimal inferredLeaveRate,
            BigDecimal renewalRate,
            Integer medianTenureDays,
            Integer reactivationCount,
            Integer earlyDepartureCount
    ) {
    }

    public record CohortRow(
            YearMonth cohortMonth,
            Integer initialStudents,
            BigDecimal m1,
            BigDecimal m2,
            BigDecimal m3,
            BigDecimal m6,
            BigDecimal m12,
            Boolean dataComplete
    ) {
    }

    public record TenureRow(
            String tenureBand,
            Integer studentCount,
            Integer inferredLeavers
    ) {
    }

    public record SegmentRow(
            String dimension,
            String dimensionValue,
            Integer studentCount,
            Integer inferredLeavers,
            BigDecimal rate
    ) {
    }

    public record ExpiryRow(
            YearMonth month,
            Integer dueEnrollments,
            Integer renewedEnrollments,
            Integer notRenewedEnrollments,
            Integer pendingWithinGrace
    ) {
    }

    public record DataQuality(
            Boolean historicalStatusAvailable,
            Integer provisionalDateCount,
            Integer missingHistoryCount
    ) {
    }

    public record StudentsRequest(
            LocalDate from,
            LocalDate to,
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth cohortMonth,
            String tenureBand,
            String segment,
            Long branchId,
            UUID trainingPlanId,
            Integer graceDays,
            Integer page,
            Integer size
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            String personStatus,
            LocalDate firstEnrollmentStartDate,
            LocalDate lastEnrollmentEndDate,
            LocalDate inferredLeaveDate,
            Integer tenureDays,
            Integer enrollmentCount,
            Integer renewalCount,
            UUID lastTrainingPlanId,
            UUID lastCourseId,
            UUID lastCourseScheduleId,
            Long lastBranchId,
            String leaveDateQuality
    ) {
    }
}
