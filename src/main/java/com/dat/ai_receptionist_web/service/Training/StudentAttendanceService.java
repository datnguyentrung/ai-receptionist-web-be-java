package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.domain.Training.StudentAttendance;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.StudentAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.StudentAttendanceAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentAttendanceService {
    private final StudentAttendanceRepository repository;
    private final StudentAttendanceMapper mapper;
    private final ClassSessionRepository classSessionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final StudentAttendanceAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<StudentAttendanceDTO.Response> list(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            UUID studentPersonId,
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
                pageable
        );
        AssignmentIndex assignmentIndex = loadAssignmentIndex(context, fromDate, toDate);
        return PageResponse.of(page, entity -> toResponse(entity, context, assignmentIndex));
    }

    @Transactional(readOnly = true)
    public StudentAttendanceDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return toResponse(findReadable(id, context), context);
    }

    @Transactional
    public StudentAttendanceDTO.Response create(StudentAttendanceDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var session = classSessionRepository.findById(request.classSessionId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        var enrollment = studentEnrollmentRepository.findById(request.studentEnrollmentId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
        CourseStaffAssignment assignment = resolveActorAssignment(
                context, session.getCourse().getCourseId(), session.getSessionDate(), true);

        personCodePolicy.requireStudent(enrollment.getStudentPerson());
        if (assignment != null) {
            personCodePolicy.requireSystemEmployee(assignment.getStaffPerson());
        }
        accessPolicy.requireCanCreate(context, session, enrollment, assignment);

        StudentAttendance entity = new StudentAttendance();
        entity.setClassSession(session);
        entity.setStudentEnrollment(enrollment);
        entity.setCourseStaffAssignment(assignment);
        entity.setCheckInTime(request.checkInTime());
        entity.setAttendanceStatus(request.attendanceStatus());
        entity.setEvaluationStatus(request.evaluationStatus());
        entity.setNote(request.note());
        return toResponse(repository.save(entity), context);
    }

    @Transactional
    public StudentAttendanceDTO.Response update(UUID id, StudentAttendanceDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        StudentAttendance entity = findManageable(id, context);
        CourseStaffAssignment assignment = resolveActorAssignment(
                context,
                entity.getClassSession().getCourse().getCourseId(),
                entity.getClassSession().getSessionDate(),
                true
        );
        accessPolicy.requireCanUpdate(context, entity, assignment);
        mapper.updateEntity(request, entity);
        return toResponse(repository.save(entity), context);
    }

    @Transactional
    public void delete(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        StudentAttendance entity = findManageable(id, context);
        CourseStaffAssignment assignment = resolveActorAssignment(
                context,
                entity.getClassSession().getCourse().getCourseId(),
                entity.getClassSession().getSessionDate(),
                true
        );
        accessPolicy.requireCanDelete(context, entity, assignment);
        repository.delete(entity);
    }

    private StudentAttendance findReadable(UUID id, AccessContext context) {
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return repository.findAccessibleById(
                id,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ATTENDANCE_NOT_FOUND));
    }

    private StudentAttendance findManageable(UUID id, AccessContext context) {
        TrainingAccessScope scope = accessPolicy.resolveWriteScope(context);
        return repository.findAccessibleById(
                id,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                false,
                false,
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ATTENDANCE_NOT_FOUND));
    }

    private CourseStaffAssignment resolveActorAssignment(
            AccessContext context,
            UUID courseId,
            LocalDate sessionDate,
            boolean required
    ) {
        if (context.hasUnrestrictedRole()) {
            return null;
        }
        if (context.activePersonId() == null) {
            if (required) {
                throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
            }
            return null;
        }
        var assignments = courseStaffAssignmentRepository.findEffectiveAssignmentsForStaffCourseOnDate(
                context.activePersonId(), courseId, sessionDate);
        if (assignments.isEmpty()) {
            if (required) {
                throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
            }
            return null;
        }
        if (assignments.size() > 1) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_AMBIGUOUS);
        }
        return assignments.getFirst();
    }

    private AssignmentIndex loadAssignmentIndex(AccessContext context, LocalDate fromDate, LocalDate toDate) {
        if (context.hasUnrestrictedRole()
                || context.activePersonId() == null
                || (!context.hasPermission(PermissionDefinition.STUDENT_ATTENDANCE_UPDATE.getCode())
                    && !context.hasPermission(PermissionDefinition.STUDENT_ATTENDANCE_DELETE.getCode()))) {
            return AssignmentIndex.empty();
        }
        return AssignmentIndex.of(courseStaffAssignmentRepository.findPolicyAssignmentsForStaffPeriod(
                context.activePersonId(), fromDate, toDate));
    }

    private StudentAttendanceDTO.Response toResponse(StudentAttendance entity, AccessContext context) {
        StudentAttendanceDTO.Response base = mapper.toResponse(entity);
        return new StudentAttendanceDTO.Response(
                base.studentAttendanceId(),
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

    private StudentAttendanceDTO.AllowedActions allowedActions(StudentAttendance entity, AccessContext context) {
        AssignmentResolver assignmentResolver = (courseId, sessionDate) ->
                resolveActorAssignment(context, courseId, sessionDate, false);
        boolean update = context.hasPermission(PermissionDefinition.STUDENT_ATTENDANCE_UPDATE.getCode())
                && canUpdate(entity, context, assignmentResolver);
        boolean delete = context.hasPermission(PermissionDefinition.STUDENT_ATTENDANCE_DELETE.getCode())
                && canDelete(entity, context, assignmentResolver);
        return new StudentAttendanceDTO.AllowedActions(update, delete);
    }

    private StudentAttendanceDTO.Response toResponse(
            StudentAttendance entity,
            AccessContext context,
            AssignmentIndex assignmentIndex
    ) {
        StudentAttendanceDTO.Response base = mapper.toResponse(entity);
        return new StudentAttendanceDTO.Response(
                base.studentAttendanceId(),
                base.classSessionId(),
                base.studentEnrollmentId(),
                base.courseStaffAssignmentId(),
                base.checkInTime(),
                base.attendanceStatus(),
                base.evaluationStatus(),
                base.note(),
                allowedActions(entity, context, assignmentIndex),
                base.createdAt(),
                base.updatedAt()
        );
    }

    private StudentAttendanceDTO.AllowedActions allowedActions(
            StudentAttendance entity,
            AccessContext context,
            AssignmentResolver assignmentResolver
    ) {
        boolean update = context.hasPermission(PermissionDefinition.STUDENT_ATTENDANCE_UPDATE.getCode())
                && canUpdate(entity, context, assignmentResolver);
        boolean delete = context.hasPermission(PermissionDefinition.STUDENT_ATTENDANCE_DELETE.getCode())
                && canDelete(entity, context, assignmentResolver);
        return new StudentAttendanceDTO.AllowedActions(update, delete);
    }

    private boolean canUpdate(
            StudentAttendance entity,
            AccessContext context,
            AssignmentResolver assignmentResolver
    ) {
        try {
            CourseStaffAssignment assignment = assignmentResolver.findEffective(
                    entity.getClassSession().getCourse().getCourseId(),
                    entity.getClassSession().getSessionDate()
            );
            accessPolicy.requireCanUpdate(context, entity, assignment);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private boolean canDelete(
            StudentAttendance entity,
            AccessContext context,
            AssignmentResolver assignmentResolver
    ) {
        try {
            CourseStaffAssignment assignment = assignmentResolver.findEffective(
                    entity.getClassSession().getCourse().getCourseId(),
                    entity.getClassSession().getSessionDate()
            );
            accessPolicy.requireCanDelete(context, entity, assignment);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private interface AssignmentResolver {
        CourseStaffAssignment findEffective(UUID courseId, LocalDate sessionDate);
    }

    private record AssignmentIndex(Map<UUID, List<CourseStaffAssignment>> byCourseId) implements AssignmentResolver {
        private static AssignmentIndex empty() {
            return new AssignmentIndex(Map.of());
        }

        private static AssignmentIndex of(List<CourseStaffAssignment> assignments) {
            Map<UUID, List<CourseStaffAssignment>> index = new HashMap<>();
            for (CourseStaffAssignment assignment : assignments) {
                UUID courseId = assignment.getCourse().getCourseId();
                index.computeIfAbsent(courseId, ignored -> new ArrayList<>()).add(assignment);
            }
            return new AssignmentIndex(index);
        }

        @Override
        public CourseStaffAssignment findEffective(UUID courseId, LocalDate sessionDate) {
            List<CourseStaffAssignment> effectiveAssignments = byCourseId
                    .getOrDefault(courseId, List.of())
                    .stream()
                    .filter(assignment -> !assignment.getStartDate().isAfter(sessionDate))
                    .filter(assignment -> assignment.getEndDate() == null
                            || !assignment.getEndDate().isBefore(sessionDate))
                    .filter(assignment -> assignment.getAssignmentStatus().allowsPolicyAccess())
                    .toList();
            if (effectiveAssignments.isEmpty()) {
                return null;
            }
            if (effectiveAssignments.size() > 1) {
                throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_AMBIGUOUS);
            }
            return effectiveAssignments.getFirst();
        }
    }
}
