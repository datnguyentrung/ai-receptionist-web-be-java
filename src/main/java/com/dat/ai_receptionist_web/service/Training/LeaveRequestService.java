package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Training.StudentSessionBooking;
import com.dat.ai_receptionist_web.domain.Training.EnrollmentUsageEntry;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.LeaveRequestDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.RequesterType;
import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import com.dat.ai_receptionist_web.enums.Training.UsageEntryType;
import com.dat.ai_receptionist_web.enums.Catalog.PlanAccessMode;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.LeaveRequestMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.LeaveRequestRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentSessionBookingRepository;
import com.dat.ai_receptionist_web.repository.Training.EnrollmentUsageEntryRepository;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanAccessRuleRepository;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owner duy nhất của vòng đời đơn xin nghỉ: tạo/truy vấn + approve/reject/cancel.
 * Approve STUDENT tạo attendance EXCUSED/MAKEUP trong cùng transaction;
 * actor/reviewer lấy từ security context.
 */
@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    private final LeaveRequestRepository repository;
    private final LeaveRequestMapper mapper;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final StudentSessionBookingRepository bookingRepository;
    private final EnrollmentUsageEntryRepository usageEntryRepository;
    private final TrainingPlanAccessRuleRepository accessRuleRepository;
    private final CurrentAccessContextResolver accessContextResolver;

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAllDetailed(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public LeaveRequestDTO.Response get(UUID id) {
        return mapper.toResponse(findDetailed(id));
    }

    @Transactional
    public LeaveRequestDTO.Response create(LeaveRequestDTO.CreateRequest request) {
        if (request.requesterType() == RequesterType.STUDENT
                && (request.leaveClassSessionId() == null || request.makeupClassSessionId() == null)) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY,
                    "Leave and makeup class session are required for STUDENT requests");
        }
        if (request.requesterType() == RequesterType.SYSTEM_EMPLOYEE
                && request.leaveDate() == null) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY,
                    "leaveDate is required for SYSTEM_EMPLOYEE requests");
        }
        LeaveRequest entity = new LeaveRequest();
        entity.setPerson(personRepository.findById(request.personId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND)));
        entity.setCreatedByUser(currentUser());
        entity.setRequesterType(request.requesterType());
        entity.setLeaveDate(request.leaveDate());
        entity.setLeaveClassSession(findClassSessionOrNull(request.leaveClassSessionId()));
        entity.setMakeupClassSession(findClassSessionOrNull(request.makeupClassSessionId()));
        requireSameCourse(entity.getLeaveClassSession(), entity.getMakeupClassSession());
        entity.setLeaveContext(request.leaveContext());
        entity.setStatus(LeaveRequestStatus.PENDING);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * A student leave must consume a real booking and reserves the makeup slot atomically.
     * The held booking is deliberately not an attendance record; only an approved request
     * may be checked in or redeemed at session settlement.
     */
    @Transactional
    public LeaveRequestDTO.Response createStudent(LeaveRequestDTO.StudentCreateRequest request) {
        StudentSessionBooking source = bookingRepository.findDetailedByIdForUpdate(request.leaveBookingId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.BOOKING_NOT_FOUND));
        if (source.getSource() == BookingSource.MAKEUP || source.getStatus() != BookingStatus.RESERVED) {
            throw new ApiException(TrainingErrorCode.MAKEUP_SOURCE_BOOKING_INVALID);
        }
        StudentEnrollment enrollment = source.getEnrollment();
        requireOwnerOrAdmin(enrollment);
        ClassSession leaveSession = source.getClassSession();
        ClassSession makeupSession = classSessionRepository.findDetailedByIdForUpdate(request.makeupClassSessionId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        requireMakeupCompatible(source, makeupSession);
        if (repository.existsByLeaveClassSession_ClassSessionIdAndPerson_PersonIdAndStatusIn(
                leaveSession.getClassSessionId(), enrollment.getStudentPerson().getPersonId(),
                java.util.List.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED))) {
            throw new ApiException(TrainingErrorCode.LEAVE_REQUEST_ALREADY_EXISTS);
        }

        LeaveRequest entity = new LeaveRequest();
        entity.setPerson(enrollment.getStudentPerson());
        entity.setCreatedByUser(currentUser());
        entity.setRequesterType(RequesterType.STUDENT);
        entity.setLeaveClassSession(leaveSession);
        entity.setMakeupClassSession(makeupSession);
        entity.setLeaveContext(request.leaveContext());
        entity.setStatus(LeaveRequestStatus.PENDING);
        entity = repository.save(entity);
        bookingRepository.save(StudentSessionBooking.builder()
                .enrollment(enrollment).classSession(makeupSession).leaveRequest(entity)
                .source(BookingSource.MAKEUP).status(BookingStatus.RESERVED).bookedAt(LocalDateTime.now()).build());
        return mapper.toResponse(entity);
    }

    @Transactional
    public LeaveRequestDTO.Response approve(UUID id, String reviewNote) {
        LeaveRequest request = lockAndRequirePending(id);
        requireAdminReviewer();
        User reviewer = currentUser();
        request.setStatus(LeaveRequestStatus.APPROVED);
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(reviewNote);
        if (request.getRequesterType() == RequesterType.STUDENT) approveStudentLeave(request);
        return mapper.toResponse(request);
    }

    @Transactional
    public LeaveRequestDTO.Response reject(UUID id, String reviewNote) {
        LeaveRequest request = lockAndRequirePending(id);
        requireAdminReviewer();
        User reviewer = currentUser();
        request.setStatus(LeaveRequestStatus.REJECTED);
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(reviewNote);
        cancelMakeupBooking(request);
        return mapper.toResponse(request);
    }

    @Transactional
    public LeaveRequestDTO.Response cancel(UUID id) {
        LeaveRequest request = lockAndRequirePending(id);
        if (request.getRequesterType() == RequesterType.STUDENT) requireOwnerOrAdminByPerson(request.getPerson().getPersonId());
        request.setStatus(LeaveRequestStatus.CANCELLED);
        cancelMakeupBooking(request);
        return mapper.toResponse(request);
    }

    private void approveStudentLeave(LeaveRequest request) {
        StudentSessionBooking makeup = bookingRepository.findByLeaveRequest_LeaveRequestId(request.getLeaveRequestId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.MAKEUP_SOURCE_BOOKING_INVALID));
        LocalDateTime makeupStart = LocalDateTime.of(makeup.getClassSession().getSessionDate(), makeup.getClassSession().getStartTime());
        if (makeup.getStatus() != BookingStatus.RESERVED || !LocalDateTime.now().isBefore(makeupStart)) {
            throw new ApiException(TrainingErrorCode.MAKEUP_RESERVATION_EXPIRED);
        }
        if (!usageEntryRepository.existsByLeaveRequest_LeaveRequestIdAndEntryType(
                request.getLeaveRequestId(), UsageEntryType.MAKEUP_GRANT)) {
            usageEntryRepository.save(EnrollmentUsageEntry.builder().enrollment(makeup.getEnrollment())
                    .booking(makeup).leaveRequest(request).entryType(UsageEntryType.MAKEUP_GRANT)
                    .units(1).occurredAt(LocalDateTime.now()).build());
        }
        attendanceRepository.findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                request.getLeaveClassSession().getClassSessionId(), makeup.getEnrollment().getStudentEnrollmentId())
                .ifPresent(attendance -> attendance.setAttendanceStatus(AttendanceStatus.EXCUSED));
    }

    private void cancelMakeupBooking(LeaveRequest request) {
        bookingRepository.findByLeaveRequest_LeaveRequestId(request.getLeaveRequestId()).ifPresent(booking -> {
            if (booking.getStatus() == BookingStatus.RESERVED) {
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
            }
        });
    }

    private void requireMakeupCompatible(StudentSessionBooking source, ClassSession makeup) {
        ClassSession leave = source.getClassSession();
        StudentEnrollment enrollment = source.getEnrollment();
        LocalDateTime leaveStart = LocalDateTime.of(leave.getSessionDate(), leave.getStartTime());
        LocalDateTime makeupStart = LocalDateTime.of(makeup.getSessionDate(), makeup.getStartTime());
        if (!LocalDateTime.now().isBefore(leaveStart) || !makeupStart.isAfter(leaveStart)
                || makeup.getStatus() == SessionStatus.CANCELLED || makeup.getStatus() == SessionStatus.COMPLETED
                || makeup.getSessionDate().isBefore(enrollment.getStartDate()) || makeup.getSessionDate().isAfter(enrollment.getEndDate())
                || bookingRepository.existsByEnrollment_StudentEnrollmentIdAndClassSession_ClassSessionId(
                        enrollment.getStudentEnrollmentId(), makeup.getClassSessionId())
                || bookingRepository.existsReservedTimeConflict(enrollment.getStudentEnrollmentId(), makeup.getSessionDate(), makeup.getStartTime(), makeup.getEndTime())
                || bookingRepository.countByClassSession_ClassSessionIdAndStatusNot(makeup.getClassSessionId(), BookingStatus.CANCELLED)
                        >= makeup.getCourse().getCapacity()) {
            throw new ApiException(TrainingErrorCode.MAKEUP_SESSION_INCOMPATIBLE);
        }
        var plan = enrollment.getCoursePurchase().getTrainingPlan();
        if (plan == null || !accessRuleRepository.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionTypeAndAccessMode(
                plan.getTrainingPlanId(), makeup.getCourse().getCourseId(), makeup.getScheduleSnapshot().getSessionType(), PlanAccessMode.FLEX)) {
            throw new ApiException(TrainingErrorCode.MAKEUP_SESSION_INCOMPATIBLE);
        }
    }

    private void requireAdminReviewer() {
        if (!accessContextResolver.current().hasUnrestrictedRole()) {
            throw new ApiException(TrainingErrorCode.LEAVE_REQUEST_REVIEW_ADMIN_REQUIRED);
        }
    }

    private void requireOwnerOrAdmin(StudentEnrollment enrollment) {
        requireOwnerOrAdminByPerson(enrollment.getStudentPerson().getPersonId());
    }

    private void requireOwnerOrAdminByPerson(UUID personId) {
        var context = accessContextResolver.current();
        if (!context.hasUnrestrictedRole() && !personId.equals(context.activePersonId())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED);
        }
    }

    private void applySessionAttendance(LeaveRequest request) {
        ClassSession leaveSession = request.getLeaveClassSession();
        ClassSession makeupSession = request.getMakeupClassSession();
        requireSameCourse(leaveSession, makeupSession);
        StudentEnrollment enrollment = enrollmentRepository
                .findActiveEnrollmentForCourseOnDate(
                        request.getPerson().getPersonId(),
                        leaveSession.getCourse().getCourseId(),
                        leaveSession.getSessionDate())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.LEAVE_ENROLLMENT_NOT_FOUND));
        upsertAttendance(leaveSession, enrollment, AttendanceStatus.EXCUSED);
        upsertAttendance(makeupSession, enrollment, AttendanceStatus.MAKEUP);
    }

    private void upsertAttendance(
            ClassSession session, StudentEnrollment enrollment, AttendanceStatus status) {
        if (attendanceRepository.existsByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                session.getClassSessionId(), enrollment.getStudentEnrollmentId())) {
            return;
        }
        attendanceRepository.save(SessionAttendance.builder()
                .classSession(session)
                .studentEnrollment(enrollment)
                .attendanceStatus(status)
                .build());
    }

    private LeaveRequest lockAndRequirePending(UUID id) {
        LeaveRequest request = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.LEAVE_REQUEST_NOT_FOUND));
        if (request.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ApiException(TrainingErrorCode.LEAVE_REQUEST_ALREADY_PROCESSED);
        }
        return request;
    }

    private void requireSameCourse(ClassSession leaveSession, ClassSession makeupSession) {
        if (leaveSession != null && makeupSession != null
                && !leaveSession.getCourse().getCourseId()
                        .equals(makeupSession.getCourse().getCourseId())) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Leave and makeup sessions must belong to the same course");
        }
    }

    private LeaveRequest find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.LEAVE_REQUEST_NOT_FOUND));
    }

    private LeaveRequest findDetailed(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.LEAVE_REQUEST_NOT_FOUND));
    }

    private User currentUser() {
        UUID userId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ApiException(SecurityErrorCode.MISSING_AUTHENTICATED_USER));
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
    }

    private ClassSession findClassSessionOrNull(UUID id) {
        return id == null ? null : classSessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
    }
}
