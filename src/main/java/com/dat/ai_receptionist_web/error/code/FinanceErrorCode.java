package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FinanceErrorCode implements ErrorCode {
    COURSE_PURCHASE_NOT_FOUND("COURSE_PURCHASE_NOT_FOUND", HttpStatus.NOT_FOUND, "Course purchase not found",
            "Course purchase not found"),
    ENROLLMENT_ADDON_PURCHASE_NOT_FOUND("ENROLLMENT_ADDON_PURCHASE_NOT_FOUND", HttpStatus.NOT_FOUND, "Enrollment addon purchase not found",
            "Enrollment addon purchase not found"),
    WALLET_NOT_FOUND("WALLET_NOT_FOUND", HttpStatus.NOT_FOUND, "Wallet not found", "Wallet not found"),
    WALLET_TRANSACTION_NOT_FOUND("WALLET_TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Wallet transaction not found", "Wallet transaction not found"),
    TRANSACTION_NOT_FOUND("TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND, "Transaction not found",
            "Original transaction not found"),
    COURSE_NOT_AVAILABLE("COURSE_NOT_AVAILABLE", HttpStatus.CONFLICT, "Course not available",
            "Course or price is inactive"),
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", HttpStatus.CONFLICT, "Insufficient balance",
            "Wallet balance is insufficient"),
    COURSE_CAPACITY_EXCEEDED("COURSE_CAPACITY_EXCEEDED", HttpStatus.CONFLICT, "Course capacity exceeded",
            "Course is full"),
    INVALID_REFUND_TRANSACTION("INVALID_REFUND_TRANSACTION", HttpStatus.CONFLICT, "Invalid refund transaction",
            "Original transaction is not an approved debit"),
    LEDGER_INVARIANT_VIOLATION("LEDGER_INVARIANT_VIOLATION", HttpStatus.CONFLICT,
            "Ledger invariant violation", "Ledger invariant violation"),
    IDEMPOTENCY_CONFLICT("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT, "Idempotency conflict",
            "External reference is already used by another operation"),
    TOP_UP_REQUEST_NOT_FOUND("TOP_UP_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "Top up request not found",
            "Top up request not found"),
    TOP_UP_REQUEST_STATE_CONFLICT("TOP_UP_REQUEST_STATE_CONFLICT", HttpStatus.CONFLICT, "Top up request state conflict",
            "Top up request is not in the required state"),
    TOP_UP_PROOF_REQUIRED("TOP_UP_PROOF_REQUIRED", HttpStatus.UNPROCESSABLE_CONTENT, "Top up proof required",
            "A payment proof image is required"),
    TOP_UP_PROOF_DUPLICATE("TOP_UP_PROOF_DUPLICATE", HttpStatus.CONFLICT, "Duplicate top up proof",
            "This proof image is already attached to another top up request"),
    TOP_UP_PROOF_ALREADY_ATTACHED("TOP_UP_PROOF_ALREADY_ATTACHED", HttpStatus.CONFLICT, "Top up proof already attached",
            "A different proof image is already attached to this request"),
    TOP_UP_RECONCILIATION_REQUIRED("TOP_UP_RECONCILIATION_REQUIRED", HttpStatus.UNPROCESSABLE_CONTENT,
            "Reconciliation confirmation required", "Confirm that the payment was reconciled before posting the top up"),
    TOP_UP_AMOUNT_LIMIT_EXCEEDED("TOP_UP_AMOUNT_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "Top up amount limit exceeded",
            "A top up cannot exceed 100,000,000 VND"),
    WALLET_NOT_ACTIVE("WALLET_NOT_ACTIVE", HttpStatus.CONFLICT, "Wallet not active", "Wallet is not active"),
    INVALID_AMOUNT("INVALID_AMOUNT", HttpStatus.BAD_REQUEST, "Invalid amount",
            "Amount must be a positive whole monetary value");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
