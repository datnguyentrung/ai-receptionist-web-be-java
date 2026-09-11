package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum TrainingErrorCode implements ErrorCode {
    CLASS_SESSION_NOT_FOUND("CLASS_SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Class session not found",
            "Class session not found"),
    COURSE_STAFF_ASSIGNMENT_NOT_FOUND("COURSE_STAFF_ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Course staff assignment not found", "Course staff assignment not found"),
    COACH_TIMESHEET_NOT_FOUND("COACH_TIMESHEET_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Coach timesheet not found", "Coach timesheet not found"),
    BELT_EXAM_NOT_FOUND("BELT_EXAM_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Belt exam not found", "Belt exam not found"),
    LEAVE_REQUEST_NOT_FOUND("LEAVE_REQUEST_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Leave request not found", "Leave request not found"),
    STUDENT_ATTENDANCE_NOT_FOUND("STUDENT_ATTENDANCE_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Student attendance not found", "Student attendance not found"),
    STUDENT_ENROLLMENT_NOT_FOUND("STUDENT_ENROLLMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Student enrollment not found", "Student enrollment not found"),
    CLASS_SESSION_IMMUTABLE("CLASS_SESSION_IMMUTABLE", HttpStatus.CONFLICT,
            "Class session immutable",
            "Class session is already started, completed or scheduled in the past"),
    CLASS_SESSION_ALREADY_EXISTS("CLASS_SESSION_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "Class session already exists",
            "A non-cancelled class session already exists for this course on the same date"),
    CLASS_SESSION_TIME_INVALID("CLASS_SESSION_TIME_INVALID", HttpStatus.BAD_REQUEST,
            "Class session time invalid", "End time must be after start time"),
    COURSE_NOT_ACTIVE("COURSE_NOT_ACTIVE", HttpStatus.CONFLICT,
            "Course not active", "Course must be ACTIVE"),
    LEAVE_REQUEST_ALREADY_PROCESSED("LEAVE_REQUEST_ALREADY_PROCESSED", HttpStatus.CONFLICT,
            "Leave request already processed", "Only PENDING leave requests can be processed"),
    LEAVE_ENROLLMENT_NOT_FOUND("LEAVE_ENROLLMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Leave enrollment not found",
            "No active student enrollment covers the leave session for this course"),
    ATTENDANCE_ALREADY_CLOSED("ATTENDANCE_ALREADY_CLOSED", HttpStatus.CONFLICT,
            "Attendance already closed", "Attendance for this class session is already closed"),
    ATTENDANCE_CLOSED("ATTENDANCE_CLOSED", HttpStatus.CONFLICT,
            "Attendance closed", "Attendance is closed and has not been reopened"),
    ATTENDANCE_UPDATE_NOT_ALLOWED("ATTENDANCE_UPDATE_NOT_ALLOWED", HttpStatus.FORBIDDEN,
            "Attendance update not allowed", "Attendance update is not allowed"),
    COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE("COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE", HttpStatus.UNPROCESSABLE_ENTITY,
            "Course staff assignment not effective", "Course staff assignment is not effective for this session"),
    COURSE_STAFF_ASSIGNMENT_AMBIGUOUS("COURSE_STAFF_ASSIGNMENT_AMBIGUOUS", HttpStatus.CONFLICT,
            "Course staff assignment ambiguous", "Multiple effective staff assignments match the same course and date"),
    STUDENT_ENROLLMENT_NOT_EFFECTIVE("STUDENT_ENROLLMENT_NOT_EFFECTIVE", HttpStatus.UNPROCESSABLE_ENTITY,
            "Student enrollment not effective", "Student enrollment is not effective for this session");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
