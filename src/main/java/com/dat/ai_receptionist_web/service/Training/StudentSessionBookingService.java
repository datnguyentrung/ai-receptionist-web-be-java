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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentSessionBookingService {
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentSessionBookingRepository bookingRepository;
    private final TrainingPlanAccessRuleRepository accessRuleRepository;
    private final EnrollmentQuotaService quotaService;

    @Transactional(readOnly = true)
    public List<StudentSessionBookingDTO.Response> list(UUID enrollmentId) {
        return bookingRepository.findDetailedByEnrollmentId(enrollmentId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSessionBookingDTO.SessionOption> options(UUID enrollmentId, LocalDate from, LocalDate to) {
        var enrollment = enrollmentRepository.findDetailedByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
        var plan = enrollment.getCoursePurchase().getTrainingPlan();
        if (plan == null) return List.of();
        return classSessionRepository.findFutureBookableCandidates(from, to).stream().map(session -> {
            String reason = optionUnavailableReason(enrollment, session);
            int remaining = Math.max(session.getCourse().getCapacity() - (int) bookingRepository
                    .countByClassSession_ClassSessionIdAndStatusNot(session.getClassSessionId(), BookingStatus.CANCELLED), 0);
            if (remaining == 0 && reason == null) reason = "Lớp đã đủ chỗ";
            return new StudentSessionBookingDTO.SessionOption(session.getClassSessionId(), session.getCourse().getCourseId(),
                    session.getCourse().getName(), session.getSessionDate(), session.getStartTime(), session.getEndTime(),
                    remaining, reason == null, reason);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSessionBookingDTO.Response> leaveOptions(UUID enrollmentId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findDetailedByEnrollmentId(enrollmentId).stream()
                .filter(booking -> booking.getStatus() == BookingStatus.RESERVED)
                .filter(booking -> booking.getSource() != BookingSource.MAKEUP)
                .filter(booking -> now.isBefore(LocalDateTime.of(booking.getClassSession().getSessionDate(), booking.getClassSession().getStartTime())))
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSessionBookingDTO.SessionOption> makeupOptions(UUID enrollmentId, UUID leaveBookingId) {
        var enrollment = enrollmentRepository.findDetailedByIdForUpdate(enrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
        var source = bookingRepository.findByStudentSessionBookingIdAndEnrollment_StudentEnrollmentId(leaveBookingId, enrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.BOOKING_NOT_FOUND));
        if (source.getStatus() != BookingStatus.RESERVED || source.getSource() == BookingSource.MAKEUP) {
            throw new ApiException(TrainingErrorCode.MAKEUP_SOURCE_BOOKING_INVALID);
        }
        return classSessionRepository.findFutureBookableCandidates(LocalDate.now(), enrollment.getEndDate()).stream()
                .map(session -> makeupOption(enrollment, source, session)).toList();
    }

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

    private String optionUnavailableReason(com.dat.ai_receptionist_web.domain.Training.StudentEnrollment enrollment,
                                           com.dat.ai_receptionist_web.domain.Training.ClassSession session) {
        var plan = enrollment.getCoursePurchase().getTrainingPlan();
        if (enrollment.getStatus() != StudentEnrollmentStatus.ACTIVE) return "Gói học chưa hoạt động";
        if (session.getSessionDate().isBefore(enrollment.getStartDate()) || session.getSessionDate().isAfter(enrollment.getEndDate())) return "Ngoài thời hạn gói học";
        if (!LocalDateTime.now().isBefore(LocalDateTime.of(session.getSessionDate(), session.getStartTime()))) return "Buổi học đã bắt đầu";
        if (!accessRuleRepository.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionTypeAndAccessMode(
                plan.getTrainingPlanId(), session.getCourse().getCourseId(), session.getScheduleSnapshot().getSessionType(), PlanAccessMode.FLEX)) return "Gói học không áp dụng cho buổi này";
        if (bookingRepository.existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(enrollment.getStudentEnrollmentId(), session.getClassSessionId())) return "Đã có booking";
        if (bookingRepository.existsReservedTimeConflict(enrollment.getStudentEnrollmentId(), session.getSessionDate(), session.getStartTime(), session.getEndTime())) return "Trùng giờ với booking khác";
        if (bookingRepository.countByClassSession_ClassSessionIdAndStatusNot(session.getClassSessionId(), BookingStatus.CANCELLED) >= session.getCourse().getCapacity()) return "Lớp đã đủ chỗ";
        if (quotaService.reservedNormalInWeek(enrollment, session.getSessionDate()) >= quotaService.weeklyQuota(enrollment, session.getSessionDate())) return "Đã đủ quota tuần";
        long committed = quotaService.normalDebits(enrollment) + bookingRepository.countNormalReserved(enrollment.getStudentEnrollmentId());
        if (committed >= quotaService.totalGranted(enrollment)) return "Đã dùng hết lượt";
        return null;
    }

    private StudentSessionBookingDTO.SessionOption makeupOption(
            com.dat.ai_receptionist_web.domain.Training.StudentEnrollment enrollment,
            StudentSessionBooking source,
            com.dat.ai_receptionist_web.domain.Training.ClassSession session) {
        String reason = null;
        LocalDateTime leaveStart = LocalDateTime.of(source.getClassSession().getSessionDate(), source.getClassSession().getStartTime());
        LocalDateTime start = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
        var plan = enrollment.getCoursePurchase().getTrainingPlan();
        if (!start.isAfter(leaveStart)) reason = "Buổi học bù phải sau buổi nghỉ";
        else if (plan == null || !accessRuleRepository.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionTypeAndAccessMode(
                plan.getTrainingPlanId(), session.getCourse().getCourseId(), session.getScheduleSnapshot().getSessionType(), PlanAccessMode.FLEX)) reason = "Gói học không áp dụng cho buổi này";
        else if (bookingRepository.existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(enrollment.getStudentEnrollmentId(), session.getClassSessionId())) reason = "Đã có booking";
        else if (bookingRepository.existsReservedTimeConflict(enrollment.getStudentEnrollmentId(), session.getSessionDate(), session.getStartTime(), session.getEndTime())) reason = "Trùng giờ với booking khác";
        int remaining = Math.max(session.getCourse().getCapacity() - (int) bookingRepository.countByClassSession_ClassSessionIdAndStatusNot(session.getClassSessionId(), BookingStatus.CANCELLED), 0);
        if (remaining == 0 && reason == null) reason = "Lớp đã đủ chỗ";
        return new StudentSessionBookingDTO.SessionOption(session.getClassSessionId(), session.getCourse().getCourseId(), session.getCourse().getName(),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), remaining, reason == null, reason);
    }
}
