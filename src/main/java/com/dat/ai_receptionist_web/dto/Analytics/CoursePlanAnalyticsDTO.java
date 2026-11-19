package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: CoursePlanAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class CoursePlanAnalyticsDTO {

    private CoursePlanAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<UUID> trainingPlanIds,
            List<String> levels,
            List<String> planStatuses
    ) {
    }

    public record Response(
            Summary summary,
            List<PlanRow> plans,
            List<CourseRow> courses,
            List<UtilizationRow> utilization,
            List<AddonRow> addons
    ) {
    }

    public record Summary(
            Integer validPurchases,
            Integer activeEnrollments,
            BigDecimal renewalRate,
            BigDecimal averagePlanDurationMonths,
            BigDecimal sessionUtilizationRate
    ) {
    }

    public record PlanRow(
            UUID trainingPlanId,
            String planName,
            String level,
            Integer durationMonths,
            Integer includedSessionCount,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            Integer validPurchaseCount,
            Integer activeStudents,
            BigDecimal renewalRate,
            Integer inferredLeavers
    ) {
    }

    public record CourseRow(
            UUID courseId,
            String courseName,
            String level,
            Integer activeStudents,
            Integer inferredLeavers,
            BigDecimal attendanceRate
    ) {
    }

    public record UtilizationRow(
            UUID trainingPlanId,
            Integer includedSessions,
            Integer usedSessions,
            BigDecimal utilizationRate
    ) {
    }

    public record AddonRow(
            UUID addonId,
            Integer extraSessionsPerWeek,
            BigDecimal price,
            Integer validUsageCount
    ) {
    }

    public record StudentsRequest(
            LocalDate from,
            LocalDate to,
            String segment,
            Long branchId,
            Integer page,
            Integer size
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            UUID coursePurchaseId,
            UUID enrollmentId,
            LocalDate enrollmentStartDate,
            LocalDate enrollmentEndDate,
            String enrollmentStatus,
            Integer includedSessionCount,
            Integer attendedSessionCount,
            BigDecimal utilizationRate,
            Boolean hasSubsequentEnrollment,
            LocalDate inferredLeaveDate,
            String personStatus
    ) {
    }
}
