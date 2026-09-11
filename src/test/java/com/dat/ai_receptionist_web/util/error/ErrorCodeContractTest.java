package com.dat.ai_receptionist_web.util.error;

import com.dat.ai_receptionist_web.error.ErrorCode;
import com.dat.ai_receptionist_web.error.code.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeContractTest {
    @Test
    void errorCodesAreUniqueAndComplete() {
        List<ErrorCode> codes = allErrorCodes().toList();

        assertThat(codes).extracting(ErrorCode::code).doesNotHaveDuplicates();
        assertThat(codes).allSatisfy(errorCode -> {
            assertThat(errorCode.code()).isNotBlank();
            assertThat(errorCode.status()).isNotNull();
            assertThat(errorCode.title()).isNotBlank();
            assertThat(errorCode.defaultDetail()).isNotBlank();
            assertThat(errorCode.type()).isNotBlank();
        });
    }

    @Test
    void publicErrorCodeStatusContractDoesNotDrift() {
        Map<String, Integer> expectedStatuses = Map.ofEntries(
                Map.entry("VALIDATION_ERROR", 400),
                Map.entry("INVALID_REQUEST_PARAMETER", 400),
                Map.entry("INVALID_REQUEST_BODY", 400),
                Map.entry("DATA_INTEGRITY_CONFLICT", 409),
                Map.entry("MEDIA_TYPE_NOT_ACCEPTABLE", 406),
                Map.entry("FILE_TOO_LARGE", 413),
                Map.entry("INTERNAL_SERVER_ERROR", 500),
                Map.entry("CLASS_SCHEDULE_NOT_FOUND", 404),
                Map.entry("COURSE_NOT_FOUND", 404),
                Map.entry("COURSE_PRICE_NOT_FOUND", 404),
                Map.entry("COURSE_SCHEDULE_PENDING_NOT_FOUND", 404),
                Map.entry("COURSE_SCHEDULE_CHANGE_CONFLICT", 409),
                Map.entry("BRANCH_NOT_FOUND", 404),
                Map.entry("PERSON_NOT_FOUND", 404),
                Map.entry("USER_PERSON_NOT_FOUND", 404),
                Map.entry("NATIONAL_CODE_ALREADY_EXISTS", 409),
                Map.entry("PERSON_CODE_ALREADY_EXISTS", 409),
                Map.entry("PERSON_CODE_POLICY_VIOLATION", 422),
                Map.entry("COURSE_PURCHASE_NOT_FOUND", 404),
                Map.entry("WALLET_NOT_FOUND", 404),
                Map.entry("WALLET_TRANSACTION_NOT_FOUND", 404),
                Map.entry("TRANSACTION_NOT_FOUND", 404),
                Map.entry("COURSE_NOT_AVAILABLE", 409),
                Map.entry("INSUFFICIENT_BALANCE", 409),
                Map.entry("COURSE_CAPACITY_EXCEEDED", 409),
                Map.entry("INVALID_REFUND_TRANSACTION", 409),
                Map.entry("LEDGER_INVARIANT_VIOLATION", 409),
                Map.entry("IDEMPOTENCY_CONFLICT", 409),
                Map.entry("WALLET_NOT_ACTIVE", 409),
                Map.entry("INVALID_AMOUNT", 400),
                Map.entry("CLASS_SESSION_NOT_FOUND", 404),
                Map.entry("COURSE_STAFF_ASSIGNMENT_NOT_FOUND", 404),
                Map.entry("COACH_TIMESHEET_NOT_FOUND", 404),
                Map.entry("BELT_EXAM_NOT_FOUND", 404),
                Map.entry("LEAVE_REQUEST_NOT_FOUND", 404),
                Map.entry("STUDENT_ATTENDANCE_NOT_FOUND", 404),
                Map.entry("STUDENT_ENROLLMENT_NOT_FOUND", 404),
                Map.entry("CLASS_SESSION_IMMUTABLE", 409),
                Map.entry("CLASS_SESSION_ALREADY_EXISTS", 409),
                Map.entry("CLASS_SESSION_TIME_INVALID", 400),
                Map.entry("COURSE_NOT_ACTIVE", 409),
                Map.entry("LEAVE_REQUEST_ALREADY_PROCESSED", 409),
                Map.entry("LEAVE_ENROLLMENT_NOT_FOUND", 404),
                Map.entry("ATTENDANCE_ALREADY_CLOSED", 409),
                Map.entry("ATTENDANCE_CLOSED", 409),
                Map.entry("ATTENDANCE_UPDATE_NOT_ALLOWED", 403),
                Map.entry("COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE", 422),
                Map.entry("COURSE_STAFF_ASSIGNMENT_AMBIGUOUS", 409),
                Map.entry("STUDENT_ENROLLMENT_NOT_EFFECTIVE", 422),
                Map.entry("FITNESS_NOT_FOUND", 404),
                Map.entry("FITNESS_RECORD_NOT_FOUND", 404),
                Map.entry("NOTIFICATION_NOT_FOUND", 404),
                Map.entry("NOTIFICATION_RECIPIENT_NOT_FOUND", 404),
                Map.entry("NOTIFICATION_RECIPIENT_NOT_ELIGIBLE", 400),
                Map.entry("NOTIFICATION_RECIPIENT_REQUIRED", 400),
                Map.entry("NOTIFICATION_RECIPIENTS_NOT_FOUND", 400),
                Map.entry("UNAUTHORIZED", 401),
                Map.entry("TOKEN_STALE", 401),
                Map.entry("ACCESS_DENIED", 403),
                Map.entry("RATE_LIMIT_EXCEEDED", 429),
                Map.entry("INVALID_CREDENTIALS", 401),
                Map.entry("USER_NOT_ACTIVE", 401),
                Map.entry("USER_NOT_FOUND", 404),
                Map.entry("PHONE_NUMBER_ALREADY_EXISTS", 409),
                Map.entry("OLD_PASSWORD_INCORRECT", 400),
                Map.entry("PASSWORD_CONFIRMATION_MISMATCH", 400),
                Map.entry("ROLE_NOT_FOUND", 404),
                Map.entry("PERMISSION_NOT_FOUND", 404),
                Map.entry("USER_ROLE_NOT_FOUND", 404),
                Map.entry("ROLE_PERMISSION_NOT_FOUND", 404),
                Map.entry("ROLE_ALREADY_EXISTS", 409),
                Map.entry("ROLES_NOT_FOUND", 400),
                Map.entry("DUPLICATE_ROLE_CODE", 409),
                Map.entry("PERMISSIONS_NOT_FOUND", 400),
                Map.entry("ROLE_PERMISSIONS_REQUIRED", 400),
                Map.entry("ROLE_CODE_REQUIRED", 400),
                Map.entry("PERMISSION_CODES_REQUIRED", 400),
                Map.entry("PERMISSION_CODES_UNDEFINED", 400),
                Map.entry("ROLE_CODES_REQUIRED", 400),
                Map.entry("MISSING_REFRESH_TOKEN", 400),
                Map.entry("INVALID_REFRESH_TOKEN", 401),
                Map.entry("REFRESH_SESSION_INACTIVE", 401),
                Map.entry("SESSION_NOT_ACTIVE", 401),
                Map.entry("SESSION_NOT_FOUND", 404),
                Map.entry("AUTH_SESSION_NOT_FOUND", 404),
                Map.entry("ACTIVE_CONTEXT_UNAVAILABLE", 401),
                Map.entry("PERSON_CONTEXT_NOT_OWNED", 403),
                Map.entry("MISSING_AUTHENTICATED_USER", 403)
        );

        Map<String, Integer> actualStatuses = allErrorCodes()
                .collect(java.util.stream.Collectors.toMap(ErrorCode::code, code -> code.status().value()));

        assertThat(actualStatuses).isEqualTo(expectedStatuses);
    }

    static Stream<ErrorCode> allErrorCodes() {
        return Stream.of(
                        GeneralErrorCode.values(),
                        CatalogErrorCode.values(),
                        CoreErrorCode.values(),
                        FinanceErrorCode.values(),
                        TrainingErrorCode.values(),
                        SkillErrorCode.values(),
                        NotificationErrorCode.values(),
                        SecurityErrorCode.values()
                )
                .flatMap(Arrays::stream)
                .map(ErrorCode.class::cast);
    }
}
