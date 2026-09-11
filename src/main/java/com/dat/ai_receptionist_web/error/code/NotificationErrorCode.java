package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "Notification not found",
            "Notification not found"),
    NOTIFICATION_RECIPIENT_NOT_FOUND("NOTIFICATION_RECIPIENT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Notification recipient not found", "Notification recipient not found"),
    NOTIFICATION_RECIPIENT_REQUIRED("NOTIFICATION_RECIPIENT_REQUIRED", HttpStatus.BAD_REQUEST,
            "Notification recipient required", "At least one notification recipient is required"),
    NOTIFICATION_RECIPIENTS_NOT_FOUND("NOTIFICATION_RECIPIENTS_NOT_FOUND", HttpStatus.BAD_REQUEST,
            "Notification recipients not found", "One or more notification recipients do not exist"),
    NOTIFICATION_RECIPIENT_NOT_ELIGIBLE("NOTIFICATION_RECIPIENT_NOT_ELIGIBLE", HttpStatus.BAD_REQUEST,
            "Notification recipient not eligible", "Notification recipient is not eligible for this context");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
