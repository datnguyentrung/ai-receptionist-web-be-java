package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.CourseStaffAssignmentMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseStaffAssignmentService {
    private final CourseStaffAssignmentRepository repository;
    private final CourseStaffAssignmentMapper mapper;
    private final PersonRepository personRepository;
    private final CourseRepository courseRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final CourseStaffAssignmentAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<CourseStaffAssignmentDTO.SimpleResponse> list(Pageable pageable) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return PageResponse.of(repository.findAccessible(
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.managedCourses(),
                pageable
        ), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public CourseStaffAssignmentDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return mapper.toResponse(findAccessible(id, context, accessPolicy.resolveReadScope(context)));
    }

    @Transactional
    public CourseStaffAssignmentDTO.Response create(CourseStaffAssignmentDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        accessPolicy.requireCanManagePeriod(
                context, request.courseId(), request.startDate(), request.endDate());
        CourseStaffAssignment entity = new CourseStaffAssignment();
        var staffPerson = personRepository.findById(request.staffPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        personCodePolicy.requireSystemEmployee(staffPerson);
        entity.setStaffPerson(staffPerson);
        entity.setCourse(courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND)));
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
        accessPolicy.requireCanManagePeriod(
                context, request.courseId(), request.startDate(), request.endDate());
        var staffPerson = personRepository.findById(request.staffPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        personCodePolicy.requireSystemEmployee(staffPerson);
        entity.setStaffPerson(staffPerson);
        entity.setCourse(courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND)));
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findAccessible(id, context, accessPolicy.resolveWriteScope(context));
        accessPolicy.requireCanManagePeriod(
                context, entity.getCourse().getCourseId(), entity.getStartDate(), entity.getEndDate());
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
}
