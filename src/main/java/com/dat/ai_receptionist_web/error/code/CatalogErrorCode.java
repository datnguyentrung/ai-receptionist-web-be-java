package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CatalogErrorCode implements ErrorCode {
    CLASS_SCHEDULE_NOT_FOUND("CLASS_SCHEDULE_NOT_FOUND", HttpStatus.NOT_FOUND, "Class schedule not found",
            "Class schedule not found"),
    COURSE_NOT_FOUND(
            "COURSE_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Course not found",
            "No course exists for the provided courseId"),
    COURSE_SCHEDULE_NOT_FOUND("COURSE_SCHEDULE_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Course schedule not found", "No course schedule exists for the provided courseId and courseScheduleId"),
    COURSE_PRICE_NOT_FOUND("COURSE_PRICE_NOT_FOUND", HttpStatus.NOT_FOUND, "Course price not found",
            "Course price not found"),
    COURSE_PRICE_IN_USE("COURSE_PRICE_IN_USE", HttpStatus.CONFLICT, "Course price is in use",
            "Course price cannot be deleted because it has course purchases"),
    TRAINING_PLAN_NOT_FOUND("TRAINING_PLAN_NOT_FOUND", HttpStatus.NOT_FOUND, "Training plan not found",
            "Training plan not found"),
    TRAINING_PLAN_INACTIVE("TRAINING_PLAN_INACTIVE", HttpStatus.CONFLICT, "Training plan inactive",
            "Training plan must be active"),
    TRAINING_PLAN_HOME_COURSE_NOT_ALLOWED("TRAINING_PLAN_HOME_COURSE_NOT_ALLOWED", HttpStatus.CONFLICT,
            "Training plan home course not allowed", "The plan cannot be purchased with this home course"),
    TRAINING_PLAN_REQUIREMENT_NOT_MET("TRAINING_PLAN_REQUIREMENT_NOT_MET", HttpStatus.UNPROCESSABLE_CONTENT,
            "Training plan requirement not met", "Recurring schedule selections do not satisfy the plan requirements"),
    COURSE_SCHEDULE_PENDING_NOT_FOUND("COURSE_SCHEDULE_PENDING_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Pending schedule change not found", "No pending schedule change exists for this course"),
    COURSE_SCHEDULE_CHANGE_CONFLICT("COURSE_SCHEDULE_CHANGE_CONFLICT", HttpStatus.CONFLICT,
            "Course schedule change conflict", "The requested course schedule change cannot be applied");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
