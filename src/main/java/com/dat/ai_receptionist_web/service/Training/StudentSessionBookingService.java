package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.StudentSessionBooking;
import com.dat.ai_receptionist_web.dto.Training.StudentSessionBookingDTO;
import com.dat.ai_receptionist_web.enums.Catalog.PlanAccessMode;
import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanAccessRuleRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentSessionBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentSessionBookingService {
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentSessionBookingRepository bookingRepository;
    private final TrainingPlanAccessRuleRepository accessRuleRepository;
    private final EnrollmentQuotaService quotaService;

    @Transactional
    public StudentSessionBookingDTO.Response bookFlexible(UUID enrollmentId, UUID sessionId) {
        var enrollment = enrollmentRepository.findDetailedByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
        var session = classSessionRepository.findDetailedByIdForUpdate(sessionId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        requireBookable(enrollment, session);

        var plan = enrollment.getCoursePurchase().getTrainingPlan();
        if (plan == null || !accessRuleRepository
                .existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionTypeAndAccessMode(
                        plan.getTrainingPlanId(), session.getCourse().getCourseId(),
                        session.getScheduleSnapshot().getSessionType(), PlanAccessMode.FLEX)) {
            throw new ApiException(TrainingErrorCode.TRAINING_PLAN_ACCESS_DENIED);
        }
        if (bookingRepository.existsReservedTimeConflict(enrollmentId, session.getSessionDate(),
                session.getStartTime(), session.getEndTime())) {
            throw new ApiException(TrainingErrorCode.BOOKING_TIME_CONFLICT);
        }
        if (bookingRepository.countByClassSession_ClassSessionIdAndStatusNot(sessionId, BookingStatus.CANCELLED)
                >= session.getCourse().getCapacity()) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_FULL);
        }
        if (quotaService.reservedNormalInWeek(enrollment, session.getSessionDate())
                >= quotaService.weeklyQuota(enrollment, session.getSessionDate())) {
            throw new ApiException(TrainingErrorCode.BOOKING_WEEKLY_QUOTA_EXCEEDED);
        }
        long committedOrReserved = quotaService.normalDebits(enrollment)
                + bookingRepository.countNormalReserved(enrollmentId);
        if (committedOrReserved >= quotaService.totalGranted(enrollment)) {
            throw new ApiException(TrainingErrorCode.BOOKING_TOTAL_QUOTA_EXCEEDED);
        }
        if (bookingRepository.existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(enrollmentId, sessionId)) {
            throw new ApiException(TrainingErrorCode.BOOKING_ALREADY_EXISTS);
        }
        StudentSessionBooking booking = bookingRepository.save(StudentSessionBooking.builder()
                .enrollment(enrollment).classSession(session).source(BookingSource.FLEX)
                .status(BookingStatus.RESERVED).bookedAt(LocalDateTime.now()).build());
        return response(booking);
    }

    @Transactional
    public void cancel(UUID enrollmentId, UUID bookingId) {
        StudentSessionBooking booking = bookingRepository
                .findByStudentSessionBookingIdAndEnrollment_StudentEnrollmentId(bookingId, enrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.BOOKING_NOT_FOUND));
        if (booking.getSource() == BookingSource.RECURRING) {
            throw new ApiException(TrainingErrorCode.SCHEDULE_CHANGE_REQUIRED);
        }
        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new ApiException(TrainingErrorCode.BOOKING_CANCELLATION_CLOSED);
        }
        LocalDateTime start = LocalDateTime.of(booking.getClassSession().getSessionDate(), booking.getClassSession().getStartTime());
        if (!LocalDateTime.now().isBefore(start)) {
            throw new ApiException(TrainingErrorCode.BOOKING_CANCELLATION_CLOSED);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
    }

    private void requireBookable(com.dat.ai_receptionist_web.domain.Training.StudentEnrollment enrollment,
                                 com.dat.ai_receptionist_web.domain.Training.ClassSession session) {
        if (enrollment.getStatus() != StudentEnrollmentStatus.ACTIVE
                || session.getSessionDate().isBefore(enrollment.getStartDate())) {
            throw new ApiException(TrainingErrorCode.ENROLLMENT_NOT_ACTIVE);
        }
        if (session.getSessionDate().isAfter(enrollment.getEndDate())) {
            throw new ApiException(TrainingErrorCode.ENROLLMENT_EXPIRED);
        }
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED
                || !LocalDateTime.now().isBefore(LocalDateTime.of(session.getSessionDate(), session.getStartTime()))) {
            throw new ApiException(TrainingErrorCode.CLASS_SESSION_IMMUTABLE);
        }
    }

    private StudentSessionBookingDTO.Response response(StudentSessionBooking booking) {
        return new StudentSessionBookingDTO.Response(booking.getStudentSessionBookingId(),
                booking.getEnrollment().getStudentEnrollmentId(), booking.getClassSession().getClassSessionId(),
                booking.getSource(), booking.getStatus(), booking.getBookedAt(), booking.getCancelledAt(), booking.getSettledAt());
    }
}
