package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum SecurityErrorCode implements ErrorCode {
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Unauthorized",
            "Authentication is required or the token is invalid"),
    TOKEN_STALE("TOKEN_STALE", HttpStatus.UNAUTHORIZED, "Token stale", "Access token is stale"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Access denied", "Access denied"),
    RATE_LIMIT_EXCEEDED("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded",
            "Too many requests"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials",
            "Invalid phone number or password"),
    USER_NOT_ACTIVE("USER_NOT_ACTIVE", HttpStatus.UNAUTHORIZED, "User not active", "User is not active"),
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "User not found"),
    PHONE_NUMBER_ALREADY_EXISTS("PHONE_NUMBER_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "Phone number already exists", "Phone number already exists"),
    OLD_PASSWORD_INCORRECT("OLD_PASSWORD_INCORRECT", HttpStatus.BAD_REQUEST, "Old password incorrect",
            "Old password is incorrect"),
    PASSWORD_CONFIRMATION_MISMATCH("PASSWORD_CONFIRMATION_MISMATCH", HttpStatus.BAD_REQUEST,
            "Password confirmation mismatch", "Password confirmation does not match"),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", HttpStatus.NOT_FOUND, "Role not found", "Role not found"),
    PERMISSION_NOT_FOUND("PERMISSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Permission not found",
            "Permission not found"),
    USER_ROLE_NOT_FOUND("USER_ROLE_NOT_FOUND", HttpStatus.NOT_FOUND, "User role not found",
            "User role not found"),
    ROLE_PERMISSION_NOT_FOUND("ROLE_PERMISSION_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Role permission not found", "Role permission not found"),
    ROLE_ALREADY_EXISTS("ROLE_ALREADY_EXISTS", HttpStatus.CONFLICT, "Role already exists",
            "Role already exists"),
    ROLES_NOT_FOUND("ROLES_NOT_FOUND", HttpStatus.BAD_REQUEST, "Roles not found",
            "One or more roles do not exist"),
    DUPLICATE_ROLE_CODE("DUPLICATE_ROLE_CODE", HttpStatus.CONFLICT, "Duplicate role code",
            "Duplicate role code"),
    PERMISSIONS_NOT_FOUND("PERMISSIONS_NOT_FOUND", HttpStatus.BAD_REQUEST, "Permissions not found",
            "One or more permissions do not exist"),
    ROLE_PERMISSIONS_REQUIRED("ROLE_PERMISSIONS_REQUIRED", HttpStatus.BAD_REQUEST,
            "Role permissions required", "Role permissions must not be null"),
    ROLE_CODE_REQUIRED("ROLE_CODE_REQUIRED", HttpStatus.BAD_REQUEST, "Role code required",
            "Role code must not be blank"),
    PERMISSION_CODES_REQUIRED("PERMISSION_CODES_REQUIRED", HttpStatus.BAD_REQUEST,
            "Permission codes required", "Permission codes must not be null"),
    PERMISSION_CODES_UNDEFINED("PERMISSION_CODES_UNDEFINED", HttpStatus.BAD_REQUEST,
            "Permission codes undefined", "One or more permission codes are not defined by the backend"),
    ROLE_CODES_REQUIRED("ROLE_CODES_REQUIRED", HttpStatus.BAD_REQUEST, "Role codes required",
            "Role codes must not be null"),
    MISSING_REFRESH_TOKEN("MISSING_REFRESH_TOKEN", HttpStatus.BAD_REQUEST, "Missing refresh token",
            "Missing refresh token"),
    INVALID_REFRESH_TOKEN("INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED, "Invalid refresh token",
            "Refresh token is invalid"),
    REFRESH_SESSION_INACTIVE("REFRESH_SESSION_INACTIVE", HttpStatus.UNAUTHORIZED,
            "Refresh session inactive", "Refresh session is not active"),
    SESSION_NOT_ACTIVE("SESSION_NOT_ACTIVE", HttpStatus.UNAUTHORIZED, "Session not active",
            "Session is not active"),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Session not found", "Session not found"),
    AUTH_SESSION_NOT_FOUND("AUTH_SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Auth session not found",
            "Auth session not found"),
    ACTIVE_CONTEXT_UNAVAILABLE("ACTIVE_CONTEXT_UNAVAILABLE", HttpStatus.UNAUTHORIZED,
            "Active context unavailable", "Active context is no longer available"),
    PERSON_CONTEXT_NOT_OWNED("PERSON_CONTEXT_NOT_OWNED", HttpStatus.FORBIDDEN,
            "Person context not owned by user", "Person context is not owned by user"),
    PASSWORD_RESET_INVALID("PASSWORD_RESET_INVALID", HttpStatus.BAD_REQUEST,
            "Password reset invalid", "The password reset code or request is invalid"),
    PASSWORD_RESET_EXPIRED("PASSWORD_RESET_EXPIRED", HttpStatus.BAD_REQUEST,
            "Password reset expired", "The password reset code has expired"),
    PASSWORD_RESET_CHANNEL_UNAVAILABLE("PASSWORD_RESET_CHANNEL_UNAVAILABLE", HttpStatus.BAD_REQUEST,
            "Password reset channel unavailable", "This password reset channel is not available"),
    PASSWORD_REUSE_NOT_ALLOWED("PASSWORD_REUSE_NOT_ALLOWED", HttpStatus.BAD_REQUEST,
            "Password reuse not allowed", "The new password must be different from the current password"),
    MISSING_AUTHENTICATED_USER("MISSING_AUTHENTICATED_USER", HttpStatus.FORBIDDEN,
            "Missing authenticated user", "Missing authenticated user");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
