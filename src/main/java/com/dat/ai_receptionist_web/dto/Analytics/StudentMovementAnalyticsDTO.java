package com.dat.ai_receptionist_web.dto.Analytics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DTO cho một page Analytics: StudentMovementAnalytics.
 * Main page và toàn bộ drill-down liên quan được gom trong cùng file.
 */
public final class StudentMovementAnalyticsDTO {

    private StudentMovementAnalyticsDTO() {
    }

    public record Request(
            LocalDate from,
            LocalDate to,
            LocalDate asOf,
            List<Long> branchIds,
            List<UUID> courseIds,
            List<UUID> courseScheduleIds,
            List<String> genders,
            Integer minAge,
            Integer maxAge,
            List<String> belts,
            List<String> levels,
            String groupBy
    ) {
    }

    public record Response(
            Summary summary,
            List<MonthlyMovement> monthly,
            List<AgeMovement> byAge,
            List<MonthlyAgeMovement> byMonthAndAge,
            List<GenderMovement> byGender,
            List<BeltMovement> byBelt,
            List<BranchMovement> byBranch,
            List<ClassMovement> byClass,
            DataQuality quality
    ) {
    }

    public record Summary(
            Integer newStudents,
            Integer reactivatedStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut,
            Integer netMovement
    ) {
    }

    public record MonthlyMovement(
            YearMonth month,
            Integer newStudents,
            Integer reactivatedStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut,
            Integer netMovement
    ) {
    }

    public record AgeMovement(
            Integer age,
            Integer newStudents,
            Integer inferredLeavers,
            Integer netMovement
    ) {
    }

    /** Dữ liệu nguồn cho ma trận tăng/giảm theo tháng và độ tuổi trên dashboard. */
    public record MonthlyAgeMovement(
            YearMonth month,
            Integer age,
            Integer newStudents,
            Integer inferredLeavers
    ) {
    }

    public record GenderMovement(
            String gender,
            Integer newStudents,
            Integer inferredLeavers,
            Integer netMovement
    ) {
    }

    public record BeltMovement(
            String belt,
            Integer newStudents,
            Integer inferredLeavers,
            Integer netMovement,
            String beltBasis
    ) {
    }

    public record BranchMovement(
            Long branchId,
            String branchName,
            Integer newStudents,
            Integer inferredLeavers,
            Integer netMovement
    ) {
    }

    public record ClassMovement(
            UUID courseScheduleId,
            String courseName,
            String scheduleLabel,
            Integer newStudents,
            Integer inferredLeavers,
            Integer transferIn,
            Integer transferOut
    ) {
    }

    public record DataQuality(
            Integer unknownLeaveDateCount,
            Integer provisionalDateCount,
            String historicalStatusLimitation
    ) {
    }

    public record StudentsRequest(
            String event,
            LocalDate from,
            LocalDate to,
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month,
            Integer age,
            String gender,
            String belt,
            Long branchId,
            UUID courseId,
            UUID courseScheduleId,
            Integer page,
            Integer size,
            String sort
    ) {
    }

    public record StudentsResponse(
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            Integer ageAtEvent,
            String gender,
            String currentBelt,
            String personStatus,
            String eventType,
            LocalDate eventDate,
            LocalDate firstEnrollmentStartDate,
            LocalDate lastEnrollmentStartDate,
            LocalDate lastEnrollmentEndDate,
            LocalDate inferredLeaveDate,
            String leaveDateSource,
            String leaveDateQuality,
            UUID courseId,
            String courseName,
            UUID courseScheduleId,
            String scheduleLabel,
            Long branchId,
            String branchName,
            String enrollmentStatus,
            Integer monthsEnrolled
    ) {
    }

    public record TimelineRequest(
            LocalDate from,
            LocalDate to,
            Boolean includeAttendance,
            Boolean includeBookings,
            Boolean includeBeltExams
    ) {
    }

    public record TimelineResponse(
            TimelineStudent student,
            TimelineLeaveSummary leaveSummary,
            List<TimelineEnrollment> enrollments,
            List<TimelineSchedule> schedules,
            List<TimelineAttendance> attendance,
            List<TimelineBooking> bookings,
            List<TimelineBeltExam> beltExams,
            List<TimelineTimelineEvent> events
    ) {
    }

    public record TimelineStudent(
            UUID personId,
            String personCode,
            String fullName,
            LocalDate birthDate,
            String gender,
            String currentBelt,
            String status,
            LocalDate startDate
    ) {
    }

    public record TimelineLeaveSummary(
            LocalDate lastEnrollmentEndDate,
            LocalDate inferredLeaveDate,
            String leaveDateSource,
            String leaveDateQuality
    ) {
    }

    public record TimelineEnrollment(
            UUID enrollmentId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            UUID coursePurchaseId,
            UUID trainingPlanId
    ) {
    }

    public record TimelineSchedule(
            UUID enrollmentId,
            UUID courseScheduleId,
            UUID courseId,
            String courseName,
            Long branchId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status
    ) {
    }

    public record TimelineAttendance(
            UUID sessionId,
            LocalDate sessionDate,
            String attendanceStatus,
            String evaluationStatus
    ) {
    }

    public record TimelineBooking(
            UUID sessionId,
            String bookingSource,
            String bookingStatus
    ) {
    }

    public record TimelineBeltExam(
            LocalDate examDate,
            String fromBelt,
            String targetBelt,
            String examType,
            String result
    ) {
    }

    public record TimelineTimelineEvent(
            LocalDate eventDate,
            String eventType,
            String sourceEntity,
            UUID sourceId,
            java.math.BigDecimal confidence
    ) {
    }
}
