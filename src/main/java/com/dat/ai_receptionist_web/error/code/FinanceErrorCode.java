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
