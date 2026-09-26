package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.SessionAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.SessionAttendanceAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionAttendanceService {
    private static final Sort DEFAULT_LIST_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("sessionAttendanceId")
    );

    private final SessionAttendanceRepository repository;
    private final SessionAttendanceMapper mapper;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final SessionAttendanceAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<SessionAttendanceDTO.SimpleResponse> list(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            UUID studentPersonId,
            UUID staffPersonId,
            Pageable pageable
    ) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        Pageable effectivePageable = withDefaultSort(pageable);
        var page = repository.findAccessible(
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses(),
                fromDate,
                toDate,
                courseId,
                studentPersonId,
                staffPersonId,
                effectivePageable
        );
        return PageResponse.of(page, mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public SessionAttendanceDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        SessionAttendance entity = findReadable(id, context);
        return mapper.toResponse(entity, allowedActions(entity, context));
    }

    @Transactional
    public SessionAttendanceDTO.Response create(SessionAttendanceDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var session = classSessionRepository.findById(request.classSessionId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        StudentEnrollment enrollment = resolveEnrollment(request.studentEnrollmentId());
        CourseStaffAssignment participantAssignment = resolveParticipantAssignment(request.courseStaffAssignmentId());

        if (enrollment != null) {
            personCodePolicy.requireStudent(enrollment.getStudentPerson());
        }
        if (participantAssignment != null) {
            personCodePolicy.requireSystemEmployee(participantAssignment.getStaffPerson());
        }
        accessPolicy.requireCanCreate(session, enrollment, participantAssignment);

        SessionAttendance entity = new SessionAttendance();
        entity.setClassSession(session);
        entity.setStudentEnrollment(enrollment);
        entity.setCourseStaffAssignment(participantAssignment);
        entity.setCheckInTime(request.checkInTime());
        entity.setAttendanceStatus(request.attendanceStatus());
        entity.setEvaluationStatus(request.evaluationStatus());
        entity.setNote(request.note());
        SessionAttendance saved = repository.save(entity);
        return mapper.toResponse(saved, allowedActions(saved, context));
    }

    @Transactional
    public SessionAttendanceDTO.Response update(UUID id, SessionAttendanceDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanUpdate(entity);
        mapper.updateEntity(request, entity);
        SessionAttendance saved = repository.save(entity);
        return mapper.toResponse(saved, allowedActions(saved, context));
    }

    @Transactional
    public void delete(UUID id) {
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanDelete(entity);
        repository.delete(entity);
    }

    @Transactional
    public SessionAttendance checkInResolvedStudent(UUID classSessionId, UUID studentEnrollmentId,
                                                    java.time.LocalDateTime checkInTime) {
        return repository.findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(
                        classSessionId, studentEnrollmentId)
                .orElseGet(() -> createResolvedStudentAttendance(classSessionId, studentEnrollmentId, checkInTime));
    }

    private SessionAttendance findReadable(UUID id, AccessContext context) {
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return repository.findAccessibleById(
                id,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND));
    }

    private SessionAttendance findManageable(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND));
    }

    private StudentEnrollment resolveEnrollment(UUID studentEnrollmentId) {
        if (studentEnrollmentId == null) {
            return null;
        }
        return studentEnrollmentRepository.findById(studentEnrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
    }

    private CourseStaffAssignment resolveParticipantAssignment(UUID courseStaffAssignmentId) {
        if (courseStaffAssignmentId == null) {
            return null;
        }
        CourseStaffAssignment assignment = courseStaffAssignmentRepository.findById(courseStaffAssignmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_FOUND));
        if (assignment.getAssignmentType() != AssignmentType.ASSISTANT_COACH) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
        return assignment;
    }

    private SessionAttendance createResolvedStudentAttendance(UUID classSessionId, UUID studentEnrollmentId,
                                                              java.time.LocalDateTime checkInTime) {
        ClassSession session = classSessionRepository.findById(classSessionId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(studentEnrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
        personCodePolicy.requireStudent(enrollment.getStudentPerson());
        accessPolicy.requireCanCreate(session, enrollment, null);
        SessionAttendance entity = baseResolvedAttendance(session, checkInTime);
        entity.setStudentEnrollment(enrollment);
        return saveResolvedAttendance(entity, () -> repository
                .findByClassSession_ClassSessionIdAndStudentEnrollment_StudentEnrollmentId(classSessionId, studentEnrollmentId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.SESSION_ATTENDANCE_NOT_FOUND)));
    }

    private SessionAttendance baseResolvedAttendance(ClassSession session, java.time.LocalDateTime checkInTime) {
        SessionAttendance entity = new SessionAttendance();
        entity.setClassSession(session);
        entity.setCheckInTime(checkInTime);
        entity.setAttendanceStatus(AttendanceStatus.PRESENT);
        entity.setEvaluationStatus(EvaluationStatus.PENDING);
        entity.setNote("FACE_CHECK_IN");
        return entity;
    }

    private SessionAttendance saveResolvedAttendance(
            SessionAttendance entity,
            java.util.function.Supplier<SessionAttendance> existingAfterConflict
    ) {
        try {
            return repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            return existingAfterConflict.get();
        }
    }

    private SessionAttendanceDTO.AllowedActions allowedActions(SessionAttendance entity, AccessContext context) {
        boolean update = context.hasPermission(PermissionDefinition.SESSION_ATTENDANCE_UPDATE.getCode())
                && canUpdate(entity);
        boolean delete = context.hasPermission(PermissionDefinition.SESSION_ATTENDANCE_DELETE.getCode())
                && canDelete(entity);
        return mapper.toAllowedActions(update, delete);
    }

    private boolean canUpdate(SessionAttendance entity) {
        try {
            accessPolicy.requireCanUpdate(entity);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private boolean canDelete(SessionAttendance entity) {
        try {
            accessPolicy.requireCanDelete(entity);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_LIST_SORT);
    }
}
