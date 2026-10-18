package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.CourseStaffAssignmentMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.CourseStaffAssignmentAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseStaffAssignmentService {
    private final CourseStaffAssignmentRepository repository;
    private final CourseStaffAssignmentMapper mapper;
    private final PersonRepository personRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final CourseStaffAssignmentAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<CourseStaffAssignmentDTO.SimpleResponse> list(
            UUID courseId,
            UUID courseScheduleId,
            Collection<AssignmentType> assignmentTypes,
            LocalDate effectiveOn,
            String search,
            Pageable pageable
    ) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        Collection<AssignmentType> resolvedTypes = assignmentTypes == null || assignmentTypes.isEmpty()
                ? List.of(AssignmentType.values())
                : assignmentTypes;
        boolean filterEffectiveOn = effectiveOn != null;
        LocalDate resolvedEffectiveOn = filterEffectiveOn
                ? effectiveOn
                : LocalDate.of(1970, 1, 1);
        return PageResponse.of(repository.findAccessible(
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.managedCourses(),
                courseId,
                courseScheduleId,
                resolvedTypes,
                filterEffectiveOn,
                resolvedEffectiveOn,
                search == null ? "" : search.trim(),
                pageable
        ), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public CourseStaffAssignmentDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return mapper.toResponse(findAccessible(id, context, accessPolicy.resolveReadScope(context)));
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<CourseStaffAssignmentDTO.BriefResponse>> getEffectiveBriefByCourseScheduleIds(
            Collection<UUID> courseScheduleIds,
            LocalDate effectiveDate
    ) {
        if (courseScheduleIds.isEmpty()) {
            return Map.of();
        }

        Comparator<CourseStaffAssignment> staffOrder = Comparator
                .comparingInt((CourseStaffAssignment assignment) -> assignment.getAssignmentType().ordinal())
                .thenComparing(assignment -> assignment.getStaffPerson().getFullName(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(CourseStaffAssignment::getCourseStaffAssignmentId);

        return repository.findEffectiveByCourseScheduleIdsAndDate(courseScheduleIds, effectiveDate).stream()
                .sorted(Comparator
                        .comparing((CourseStaffAssignment assignment) ->
                                assignment.getCourseSchedule().getCourseScheduleId())
                        .thenComparing(staffOrder))
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getCourseSchedule().getCourseScheduleId(),
                        LinkedHashMap::new,
                        Collectors.mapping(mapper::toBriefResponse, Collectors.toList())
                ));
    }

    @Transactional
    public CourseStaffAssignmentDTO.Response create(CourseStaffAssignmentDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var courseSchedule = courseScheduleRepository.findDetailedById(request.courseScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND));
        accessPolicy.requireCanManagePeriod(
                context, courseSchedule.getCourse().getCourseId(), request.startDate(), request.endDate());
        validateAssignment(request.courseScheduleId(), request.assignmentType(), request.startDate(),
                request.endDate(), request.assignmentStatus(), null, courseSchedule.getStartDate(), courseSchedule.getEndDate());
        CourseStaffAssignment entity = new CourseStaffAssignment();
        var staffPerson = personRepository.findById(request.staffPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        personCodePolicy.requireSystemEmployee(staffPerson);
        entity.setStaffPerson(staffPerson);
        entity.setCourseSchedule(courseSchedule);
        entity.setAssignmentType(request.assignmentType());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setAssignmentStatus(request.assignmentStatus());
        entity.setNote(request.note());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CourseStaffAssignmentDTO.Response update(UUID id, CourseStaffAssignmentDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        var courseSchedule = courseScheduleRepository.findDetailedById(request.courseScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND));
        accessPolicy.requireCanManagePeriod(
                context, courseSchedule.getCourse().getCourseId(), request.startDate(), request.endDate());
        validateAssignment(request.courseScheduleId(), request.assignmentType(), request.startDate(),
                request.endDate(), request.assignmentStatus(), id, courseSchedule.getStartDate(), courseSchedule.getEndDate());
        var staffPerson = personRepository.findById(request.staffPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        personCodePolicy.requireSystemEmployee(staffPerson);
        entity.setStaffPerson(staffPerson);
        entity.setCourseSchedule(courseSchedule);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public CourseStaffAssignmentDTO.Response end(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        var courseSchedule = entity.getCourseSchedule();
        LocalDate endDate = LocalDate.now();
        if (endDate.isBefore(entity.getStartDate())) {
            endDate = entity.getStartDate();
        }
        if (courseSchedule.getEndDate() != null && endDate.isAfter(courseSchedule.getEndDate())) {
            endDate = courseSchedule.getEndDate();
        }
        accessPolicy.requireCanManagePeriod(
                context, courseSchedule.getCourse().getCourseId(), entity.getStartDate(), endDate);
        entity.setEndDate(endDate);
        entity.setAssignmentStatus(CourseStaffAssignmentStatus.ENDED);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        accessPolicy.requireCanManagePeriod(
                context, entity.getCourseSchedule().getCourse().getCourseId(), entity.getStartDate(), entity.getEndDate());
        entity.setAssignmentStatus(CourseStaffAssignmentStatus.CANCELLED);
    }

    private CourseStaffAssignment findAccessible(
            UUID id,
            AccessContext context,
            TrainingAccessScope scope
    ) {
        return repository.findAccessibleById(
                id,
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.managedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_NOT_FOUND));
    }

    private void validateAssignment(
            UUID courseScheduleId,
            AssignmentType assignmentType,
            LocalDate startDate,
            LocalDate endDate,
            CourseStaffAssignmentStatus assignmentStatus,
            UUID excludeId,
            LocalDate scheduleStartDate,
            LocalDate scheduleEndDate
    ) {
        if ((endDate != null && endDate.isBefore(startDate))
                || startDate.isBefore(scheduleStartDate)
                || (scheduleEndDate != null && endDate != null && endDate.isAfter(scheduleEndDate))
                || (assignmentStatus == CourseStaffAssignmentStatus.ENDED && endDate == null)) {
            throw new ApiException(TrainingErrorCode.COURSE_STAFF_ASSIGNMENT_PERIOD_INVALID);
        }
        if (assignmentType == AssignmentType.PRIMARY_COACH
                && repository.existsOverlappingPrimaryAssignment(courseScheduleId, startDate, endDate, excludeId)) {
            throw new ApiException(TrainingErrorCode.PRIMARY_COACH_ASSIGNMENT_CONFLICT);
        }
    }
}
