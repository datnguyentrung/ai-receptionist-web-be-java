package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.LeaveRequestDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.RequesterType;
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

    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public LeaveRequestDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
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

    @Transactional
    public LeaveRequestDTO.Response approve(UUID id, String reviewNote) {
        LeaveRequest request = lockAndRequirePending(id);
        User reviewer = currentUser();
        request.setStatus(LeaveRequestStatus.APPROVED);
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(reviewNote);
        if (request.getRequesterType() == RequesterType.STUDENT) {
            applySessionAttendance(request);
        }
        return mapper.toResponse(request);
    }

    @Transactional
    public LeaveRequestDTO.Response reject(UUID id, String reviewNote) {
        LeaveRequest request = lockAndRequirePending(id);
        User reviewer = currentUser();
        request.setStatus(LeaveRequestStatus.REJECTED);
        request.setReviewedByUser(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewNote(reviewNote);
        return mapper.toResponse(request);
    }

    @Transactional
    public LeaveRequestDTO.Response cancel(UUID id) {
        LeaveRequest request = lockAndRequirePending(id);
        request.setStatus(LeaveRequestStatus.CANCELLED);
        return mapper.toResponse(request);
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
