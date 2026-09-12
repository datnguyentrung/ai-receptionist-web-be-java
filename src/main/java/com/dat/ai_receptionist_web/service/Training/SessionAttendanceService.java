package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionAttendanceService {
    private final SessionAttendanceRepository repository;
    private final SessionAttendanceMapper mapper;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final SessionAttendanceAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<SessionAttendanceDTO.Response> list(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            UUID studentPersonId,
            UUID staffPersonId,
            Pageable pageable
    ) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
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
                pageable
        );
        return PageResponse.of(page, entity -> toResponse(entity, context));
    }

    @Transactional(readOnly = true)
    public SessionAttendanceDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return toResponse(findReadable(id, context), context);
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
        return toResponse(repository.save(entity), context);
    }

    @Transactional
    public SessionAttendanceDTO.Response update(UUID id, SessionAttendanceDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanUpdate(entity);
        mapper.updateEntity(request, entity);
        return toResponse(repository.save(entity), context);
    }

    @Transactional
    public void delete(UUID id) {
        SessionAttendance entity = findManageable(id);
        accessPolicy.requireCanDelete(entity);
        repository.delete(entity);
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

    private SessionAttendanceDTO.Response toResponse(SessionAttendance entity, AccessContext context) {
        SessionAttendanceDTO.Response base = mapper.toResponse(entity);
        return new SessionAttendanceDTO.Response(
                base.sessionAttendanceId(),
                base.classSessionId(),
                base.studentEnrollmentId(),
                base.courseStaffAssignmentId(),
                base.checkInTime(),
                base.attendanceStatus(),
                base.evaluationStatus(),
                base.note(),
                allowedActions(entity, context),
                base.createdAt(),
                base.updatedAt()
        );
    }

    private SessionAttendanceDTO.AllowedActions allowedActions(SessionAttendance entity, AccessContext context) {
        boolean update = context.hasPermission(PermissionDefinition.SESSION_ATTENDANCE_UPDATE.getCode())
                && canUpdate(entity);
        boolean delete = context.hasPermission(PermissionDefinition.SESSION_ATTENDANCE_DELETE.getCode())
                && canDelete(entity);
        return new SessionAttendanceDTO.AllowedActions(update, delete);
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
}
