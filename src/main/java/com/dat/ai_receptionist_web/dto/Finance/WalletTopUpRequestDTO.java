package com.dat.ai_receptionist_web.dto.Finance;

import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestMode;
import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class WalletTopUpRequestDTO {
    private WalletTopUpRequestDTO() {}

    public record CreateRequest(
            @NotNull UUID clientRequestId,
            @NotNull UUID personId,
            @NotNull @Positive @DecimalMax("100000000") BigDecimal amount,
            @Size(max = 500) String note) {}

    public record SubmitRequest(boolean reconciliationConfirmed) {}

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {}

    public record Response(
            UUID topUpRequestId,
            UUID clientRequestId,
            UUID personId,
            String personCode,
            String studentName,
            WalletTopUpRequestMode mode,
            WalletTopUpRequestStatus status,
            BigDecimal amount,
            String note,
            String rejectionReason,
            boolean proofAttached,
            UUID walletTransactionId,
            BigDecimal balanceAfter,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {}

    public record ProofResponse(UUID topUpRequestId, boolean proofAttached) {}

    public record ProofUrlResponse(String url, long expiresInSeconds) {}
}

