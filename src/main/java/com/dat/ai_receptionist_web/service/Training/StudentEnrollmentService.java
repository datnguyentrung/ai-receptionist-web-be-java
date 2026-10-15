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
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentService {
    private final StudentEnrollmentRepository repository;
    private final StudentEnrollmentMapper mapper;
    private final PersonRepository personRepository;
    private final CoursePurchaseRepository coursePurchaseRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final StudentEnrollmentScheduleRepository studentEnrollmentScheduleRepository;
    private final CourseScheduleMapper courseScheduleMapper;
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
        var enrollments = repository.findAccessible(
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
        Map<UUID, List<com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.SimpleResponse>>
                schedulesByEnrollmentId = getSimpleSchedulesByEnrollmentIds(
                enrollments.getContent().stream()
                        .map(com.dat.ai_receptionist_web.domain.Training.StudentEnrollment::getStudentEnrollmentId)
                        .toList());
        return PageResponse.of(enrollments, enrollment -> mapper.toSimpleResponse(
                enrollment,
                schedulesByEnrollmentId.getOrDefault(enrollment.getStudentEnrollmentId(), List.of())));
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
        UUID courseId = purchase.getCoursePrice().getCourse().getCourseId();
        accessPolicy.requireCanManageCoursePeriod(context, courseId, request.startDate(), request.endDate());

        var entity = new com.dat.ai_receptionist_web.domain.Training.StudentEnrollment();
        entity.setStudentPerson(studentPerson);
        entity.setCoursePurchase(purchase);
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setStatus(request.status());
        entity.replaceSchedules(resolveSchedules(courseId, request.courseScheduleIds(), request.startDate(), request.endDate()));
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
        UUID courseId = purchase.getCoursePrice().getCourse().getCourseId();
        accessPolicy.requireCanManageCoursePeriod(context, courseId, request.startDate(), request.endDate());

        entity.setStudentPerson(studentPerson);
        entity.setCoursePurchase(purchase);
        entity.replaceSchedules(resolveSchedules(courseId, request.courseScheduleIds(), request.startDate(), request.endDate()));
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

    private LinkedHashSet<com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule> resolveSchedules(
            UUID courseId, java.util.List<UUID> scheduleIds, LocalDate start, LocalDate end) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT, "At least one course schedule is required");
        }
        LinkedHashSet<UUID> uniqueScheduleIds = new LinkedHashSet<>(scheduleIds);
        Map<UUID, com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule> schedulesById =
                courseScheduleRepository.findDetailedByIds(uniqueScheduleIds).stream()
                        .collect(Collectors.toMap(
                                com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule::getCourseScheduleId,
                                schedule -> schedule));
        if (schedulesById.size() != uniqueScheduleIds.size()) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND);
        }

        LinkedHashSet<com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule> links = new LinkedHashSet<>();
        for (UUID id : uniqueScheduleIds) {
            var courseSchedule = schedulesById.get(id);
            if (!courseSchedule.getCourse().getCourseId().equals(courseId)
                    || courseSchedule.getStartDate().isAfter(end)
                    || (courseSchedule.getEndDate() != null && courseSchedule.getEndDate().isBefore(start))) {
                throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT, "Course schedule does not apply to enrollment period");
            }
            links.add(com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule.builder()
                    .courseSchedule(courseSchedule).build());
        }
        return links;
    }

    private Map<UUID, List<com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.SimpleResponse>>
    getSimpleSchedulesByEnrollmentIds(List<UUID> enrollmentIds) {
        if (enrollmentIds.isEmpty()) {
            return Map.of();
        }
        return studentEnrollmentScheduleRepository.findDetailedByStudentEnrollmentIds(enrollmentIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getStudentEnrollment().getStudentEnrollmentId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                link -> courseScheduleMapper.toSimpleResponse(link.getCourseSchedule()),
                                Collectors.toList())));
    }
}
