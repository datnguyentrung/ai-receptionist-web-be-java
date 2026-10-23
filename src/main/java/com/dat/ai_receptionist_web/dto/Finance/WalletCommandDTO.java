package com.dat.ai_receptionist_web.dto.Finance;

import com.dat.ai_receptionist_web.enums.Finance.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public final class WalletCommandDTO {
    private WalletCommandDTO() {
    }

    public record TopUpRequest(
            @NotNull UUID personId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank @Size(max = 150) String externalReference,
            @Size(max = 500) String note) {
    }

    public record RecurringScheduleSelection(
            @NotNull UUID requirementId,
            @NotNull UUID courseScheduleId) {
    }

    public record TrainingPlanPurchaseRequest(
            @NotNull UUID studentPersonId,
            @NotNull UUID trainingPlanId,
            @NotNull UUID homeCourseId,
            @NotNull java.time.LocalDate startDate,
            @NotNull List<@jakarta.validation.Valid RecurringScheduleSelection> recurringSchedules,
            @NotBlank @Size(max = 150) String externalReference,
            @Size(max = 500) String note) {
    }

    public record RefundRequest(
            @NotNull UUID originalDebitTransactionId,
            @Size(max = 500) String note) {
    }

    public record TransactionResponse(
            UUID walletTransactionId,
            UUID walletId,
            WalletTransactionType type,
            WalletTransactionDirection direction,
            WalletTransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String externalReference,
            UUID coursePurchaseId,
            UUID studentEnrollmentId,
            LocalDateTime reviewedAt) {
    }
}
