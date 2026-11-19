package com.dat.ai_receptionist_web.dto.Analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO cho một page Analytics: FinanceAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class FinanceAnalyticsDTO {

    private FinanceAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            List<Long> branchIds,
            List<UUID> trainingPlanIds,
            List<String> transactionTypes,
            List<String> directions,
            List<String> statuses
    ) {
    }

    public record Response(
            Summary summary,
            List<MonthlyRow> monthly,
            List<PlanRow> byPlan,
            List<BranchRow> byBranch,
            List<StatusRow> byStatus,
            DataQuality quality
    ) {
    }

    public record Summary(
            BigDecimal approvedTopUpAmount,
            BigDecimal approvedPlanPurchaseAmount,
            BigDecimal approvedAddonPurchaseAmount,
            BigDecimal approvedRefundAmount,
            Integer approvedTransactionCount,
            Integer excludedMigrationTransactionCount
    ) {
    }

    public record MonthlyRow(
            YearMonth month,
            BigDecimal topUpAmount,
            BigDecimal planPurchaseAmount,
            BigDecimal addonPurchaseAmount,
            BigDecimal refundAmount
    ) {
    }

    public record PlanRow(
            UUID trainingPlanId,
            Integer validPurchaseCount,
            BigDecimal approvedPurchaseAmount
    ) {
    }

    public record BranchRow(
            Long branchId,
            BigDecimal attributedPurchaseAmount,
            String attributionMethod,
            BigDecimal unattributedAmount
    ) {
    }

    public record StatusRow(
            String transactionStatus,
            Integer transactionCount,
            BigDecimal totalAmount
    ) {
    }

    public record DataQuality(
            Integer excludedPendingCount,
            Integer excludedSyntheticCount,
            Integer unattributedPurchaseCount
    ) {
    }

    public record TransactionsRequest(
            LocalDate from,
            LocalDate to,
            String transactionType,
            String direction,
            String status,
            UUID trainingPlanId,
            Long branchId,
            Boolean includeExcluded,
            Integer page,
            Integer size
    ) {
    }

    public record TransactionsResponse(
            UUID transactionId,
            UUID personId,
            String transactionType,
            String direction,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String status,
            LocalDateTime createdAt,
            LocalDateTime reviewedAt,
            UUID coursePurchaseId,
            UUID trainingPlanId,
            Long attributedBranchId,
            Boolean isMigrationSynthetic,
            Boolean isIncludedInAnalytics,
            String exclusionReason
    ) {
    }
}
