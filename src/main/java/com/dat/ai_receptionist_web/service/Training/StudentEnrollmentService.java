package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentDTO;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.FinanceErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.StudentEnrollmentMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.PersonCodePolicy;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.StudentEnrollmentAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentService {
    private final StudentEnrollmentRepository repository;
    private final StudentEnrollmentMapper mapper;
    private final PersonRepository personRepository;
    private final CoursePurchaseRepository coursePurchaseRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final PersonCodePolicy personCodePolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final StudentEnrollmentAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<StudentEnrollmentDTO.SimpleResponse> list(
            LocalDate fromDate,
            LocalDate toDate,
            UUID courseId,
            UUID studentPersonId,
            Pageable pageable
    ) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return PageResponse.of(repository.findAccessible(
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
        ), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public StudentEnrollmentDTO.Response get(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return mapper.toResponse(repository.findAccessibleById(
                id,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND)));
    }

    @Transactional
    public StudentEnrollmentDTO.Response create(StudentEnrollmentDTO.CreateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var studentPerson = personRepository.findById(request.studentPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        personCodePolicy.requireStudent(studentPerson);
        var purchase = coursePurchaseRepository.findById(request.coursePurchaseId())
                .orElseThrow(() -> new ApiException(FinanceErrorCode.COURSE_PURCHASE_NOT_FOUND));
        var schedule = classScheduleRepository.findById(request.classScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));

        UUID courseId = purchase.getCoursePrice().getCourse().getCourseId();
        accessPolicy.requireCanManageCoursePeriod(context, courseId, request.startDate(), request.endDate());

        var entity = new com.dat.ai_receptionist_web.domain.Training.StudentEnrollment();
        entity.setStudentPerson(studentPerson);
        entity.setCoursePurchase(purchase);
        entity.setClassSchedule(schedule);
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setStatus(request.status());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public StudentEnrollmentDTO.Response update(UUID id, StudentEnrollmentDTO.UpdateRequest request) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findManageable(id, context);
        var studentPerson = personRepository.findById(request.studentPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        personCodePolicy.requireStudent(studentPerson);
        var purchase = coursePurchaseRepository.findById(request.coursePurchaseId())
                .orElseThrow(() -> new ApiException(FinanceErrorCode.COURSE_PURCHASE_NOT_FOUND));
        var schedule = classScheduleRepository.findById(request.classScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));

        UUID courseId = purchase.getCoursePrice().getCourse().getCourseId();
        accessPolicy.requireCanManageCoursePeriod(context, courseId, request.startDate(), request.endDate());

        entity.setStudentPerson(studentPerson);
        entity.setCoursePurchase(purchase);
        entity.setClassSchedule(schedule);
        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        var entity = findManageable(id, context);
        entity.setStatus(StudentEnrollmentStatus.CANCELLED);
    }

    private com.dat.ai_receptionist_web.domain.Training.StudentEnrollment findManageable(
            UUID id,
            AccessContext context
    ) {
        TrainingAccessScope scope = accessPolicy.resolveWriteScope(context);
        return repository.findAccessibleById(
                id,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                false,
                false,
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
    }
}