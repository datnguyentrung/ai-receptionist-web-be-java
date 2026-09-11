package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.CoachTimesheetMapper;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CoachTimesheetRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.CoachTimesheetAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachTimesheetService {
    private final CoachTimesheetRepository repository;
    private final CoachTimesheetMapper mapper;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final CoachTimesheetAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<CoachTimesheetDTO.Response> list(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            Pageable pageable
    ) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return PageResponse.of(repository.findAccessible(
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.managedCourses(),
                fromDate,
                toDate,
                courseId,
                pageable
        ), entity -> toResponse(entity, context));
    }

    @Transactional(readOnly = true)
    public CoachTimesheetDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return toResponse(findAccessible(id, context), context);
    }

    @Transactional
    public CoachTimesheetDTO.Response create(CoachTimesheetDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        CoachTimesheet entity = new CoachTimesheet();
        var session = classSessionRepository.findById(request.classSessionId())
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
        CourseStaffAssignment assignment = resolveAssignment(context, session.getCourse().getCourseId(),
                session.getSessionDate());
        personCodePolicy.requireSystemEmployee(assignment.getStaffPerson());
        accessPolicy.requireCanCreate(context, session, assignment);
        entity.setCourseStaffAssignment(assignment);
        entity.setClassSession(session);
        entity.setCheckInTime(request.checkInTime());
        entity.setCheckOutTime(request.checkOutTime());
        entity.setNote(request.note());
        return toResponse(repository.save(entity), context);
    }

    @Transactional
    public CoachTimesheetDTO.Response update(UUID id, CoachTimesheetDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findManageable(id, context);
        accessPolicy.requireCanUpdate(context, entity);
        mapper.updateEntity(request, entity);
        return toResponse(repository.save(entity), context);
    }

    @Transactional
    public void delete(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findManageable(id, context);
        accessPolicy.requireCanDelete(context, entity);
        repository.delete(entity);
    }

    private CoachTimesheet findAccessible(UUID id, AccessContext context) {
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return repository.findAccessibleById(
                id,
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.managedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.COACH_TIMESHEET_NOT_FOUND));
    }

    private CoachTimesheet findManageable(UUID id, AccessContext context) {
        TrainingAccessScope scope = accessPolicy.resolveWriteScope(context);
        return repository.findAccessibleById(
                id,
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                false
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.COACH_TIMESHEET_NOT_FOUND));
    }

    private CourseStaffAssignment resolveAssignment(AccessContext context, UUID courseId, LocalDate sessionDate) {
        if (context.activePersonId() == null) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
        var assignments = courseStaffAssignmentRepository.findEffectiveAssignmentsForStaffCourseOnDate(
                context.activePersonId(),
                courseId,
                sessionDate
        );
        if (assignments.isEmpty()) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_EFFECTIVE);
        }
        if (assignments.size() > 1) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_AMBIGUOUS);
        }
        return assignments.getFirst();
    }

    private CoachTimesheetDTO.Response toResponse(CoachTimesheet entity, AccessContext context) {
        CoachTimesheetDTO.Response base = mapper.toResponse(entity);
        return new CoachTimesheetDTO.Response(
                base.coachTimesheetId(),
                base.courseStaffAssignmentId(),
                base.classSessionId(),
                base.checkInTime(),
                base.checkOutTime(),
                base.note(),
                allowedActions(entity, context),
                base.createdAt(),
                base.updatedAt()
        );
    }

    private CoachTimesheetDTO.AllowedActions allowedActions(CoachTimesheet entity, AccessContext context) {
        boolean update = context.hasPermission(PermissionDefinition.COACH_TIMESHEET_UPDATE.getCode())
                && canUpdate(entity, context);
        boolean delete = context.hasPermission(PermissionDefinition.COACH_TIMESHEET_DELETE.getCode())
                && canDelete(entity, context);
        return new CoachTimesheetDTO.AllowedActions(update, delete);
    }

    private boolean canUpdate(CoachTimesheet entity, AccessContext context) {
        try {
            accessPolicy.requireCanUpdate(context, entity);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }

    private boolean canDelete(CoachTimesheet entity, AccessContext context) {
        try {
            accessPolicy.requireCanDelete(context, entity);
            return true;
        } catch (ApiException ex) {
            return false;
        }
    }
}
