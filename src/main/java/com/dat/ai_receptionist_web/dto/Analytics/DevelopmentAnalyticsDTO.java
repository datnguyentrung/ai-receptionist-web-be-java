package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: DevelopmentAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class DevelopmentAnalyticsDTO {

    private DevelopmentAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<String> belts,
            List<String> levels,
            List<String> examTypes,
            List<String> examResults,
            Integer minAge,
            Integer maxAge
    ) {
    }

    public record Response(
            Summary summary,
            List<BeltDistributionRow> beltDistribution,
            List<ExamTrendRow> examTrend,
            List<AgeRow> byAge,
            List<BranchRow> byBranch,
            List<FitnessRow> fitness
    ) {
    }

    public record Summary(
            Integer examParticipants,
            Integer examAttempts,
            Integer passedAttempts,
            BigDecimal passRate,
            Integer fitnessParticipants,
            Integer fitnessRecordCount
    ) {
    }

    public record BeltDistributionRow(
            String belt,
            Integer studentCount
    ) {
    }

    public record ExamTrendRow(
            YearMonth month,
            String examType,
            Integer passed,
            Integer failed,
            Integer absent,
            Integer pending
    ) {
    }

    public record AgeRow(
            Integer age,
            Integer examAttempts,
            BigDecimal passRate
    ) {
    }

    public record BranchRow(
            Long branchId,
            Integer examAttempts,
            BigDecimal passRate
    ) {
    }

    public record FitnessRow(
            Long fitnessId,
            String level,
            Integer recordCount,
            Integer participantCount,
            BigDecimal averageDuration
    ) {
    }

    public record StudentsRequest(
            LocalDate from,
            LocalDate to,
            Long branchId,
            String belt,
            String examResult,
            String examType,
            UUID fitnessId,
            Integer page,
            Integer size
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            String currentBelt,
            LocalDate examDate,
            String fromBelt,
            String targetBelt,
            String examType,
            String examResult,
            Long fitnessId,
            LocalDate fitnessRecordDate,
            BigDecimal fitnessDuration
    ) {
    }
}
