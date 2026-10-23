package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollmentSchedule;
import com.dat.ai_receptionist_web.dto.Training.StudentEnrollmentScheduleDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.StudentEnrollmentScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.StudentEnrollmentAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentEnrollmentScheduleService {
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final StudentEnrollmentScheduleRepository repository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final StudentEnrollmentScheduleMapper mapper;
    private final CurrentAccessContextResolver currentAccessContextResolver;
    private final StudentEnrollmentAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<StudentEnrollmentScheduleDTO.SimpleResponse> list(UUID enrollmentId) {
        findReadableEnrollment(enrollmentId);
        return repository.findDetailedByStudentEnrollmentId(enrollmentId).stream()
                .map(mapper::toSimpleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentEnrollmentScheduleDTO.Response get(UUID id) {
        StudentEnrollmentSchedule link = findDetailed(id);
        findReadableEnrollment(link.getStudentEnrollment().getStudentEnrollmentId());
        return mapper.toResponse(link);
    }

    @Transactional(readOnly = true)
    public StudentEnrollmentScheduleDTO.Response get(UUID enrollmentId, UUID id) {
        findReadableEnrollment(enrollmentId);
        return repository.findDetailedByStudentEnrollmentIdAndId(enrollmentId, id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
    }

    @Transactional
    public StudentEnrollmentScheduleDTO.Response create(
            UUID enrollmentId, StudentEnrollmentScheduleDTO.UpsertRequest request) {
        StudentEnrollment enrollment = findManageableEnrollment(enrollmentId);
        if (repository.existsByStudentEnrollment_StudentEnrollmentIdAndCourseSchedule_CourseScheduleId(
                enrollmentId, request.courseScheduleId())) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Course schedule is already linked to this enrollment");
        }
        StudentEnrollmentSchedule link = StudentEnrollmentSchedule.builder()
                .studentEnrollment(enrollment)
                .courseSchedule(resolveApplicableSchedule(enrollment, request.courseScheduleId()))
                .build();
        return mapper.toResponse(repository.save(link));
    }

    @Transactional
    public StudentEnrollmentScheduleDTO.Response create(StudentEnrollmentScheduleDTO.CreateRequest request) {
        return create(request.studentEnrollmentId(), new StudentEnrollmentScheduleDTO.UpsertRequest(request.courseScheduleId()));
    }

    @Transactional
    public StudentEnrollmentScheduleDTO.Response update(
            UUID enrollmentId, UUID id, StudentEnrollmentScheduleDTO.UpsertRequest request) {
        StudentEnrollment enrollment = findManageableEnrollment(enrollmentId);
        StudentEnrollmentSchedule link = repository.findDetailedByStudentEnrollmentIdAndId(enrollmentId, id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
        if (!link.getCourseSchedule().getCourseScheduleId().equals(request.courseScheduleId())
                && repository.existsByStudentEnrollment_StudentEnrollmentIdAndCourseSchedule_CourseScheduleId(
                enrollmentId, request.courseScheduleId())) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Course schedule is already linked to this enrollment");
        }
        link.setCourseSchedule(resolveApplicableSchedule(enrollment, request.courseScheduleId()));
        return mapper.toResponse(repository.save(link));
    }

    @Transactional
    public StudentEnrollmentScheduleDTO.Response update(UUID id, StudentEnrollmentScheduleDTO.UpdateRequest request) {
        StudentEnrollmentSchedule link = findDetailed(id);
        return update(link.getStudentEnrollment().getStudentEnrollmentId(), id,
                new StudentEnrollmentScheduleDTO.UpsertRequest(request.courseScheduleId()));
    }

    @Transactional
    public void delete(UUID enrollmentId, UUID id) {
        findManageableEnrollment(enrollmentId);
        StudentEnrollmentSchedule link = repository.findDetailedByStudentEnrollmentIdAndId(enrollmentId, id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
        if (repository.countByStudentEnrollment_StudentEnrollmentId(enrollmentId) <= 1) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "An enrollment must retain at least one course schedule");
        }
        repository.delete(link);
    }

    @Transactional
    public void delete(UUID id) {
        StudentEnrollmentSchedule link = findDetailed(id);
        delete(link.getStudentEnrollment().getStudentEnrollmentId(), id);
    }

    private StudentEnrollment findReadableEnrollment(UUID enrollmentId) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveReadScope(context);
        return findAccessibleEnrollment(enrollmentId, context, scope);
    }

    private StudentEnrollmentSchedule findDetailed(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_SCHEDULE_NOT_FOUND));
    }

    private StudentEnrollment findManageableEnrollment(UUID enrollmentId) {
        AccessContext context = currentAccessContextResolver.current();
        TrainingAccessScope scope = accessPolicy.resolveWriteScope(context);
        return findAccessibleEnrollment(enrollmentId, context, scope);
    }

    private StudentEnrollment findAccessibleEnrollment(
            UUID enrollmentId, AccessContext context, TrainingAccessScope scope) {
        return studentEnrollmentRepository.findAccessibleById(
                enrollmentId,
                context.userId(),
                context.activePersonId(),
                scope.unrestricted(),
                scope.self(),
                scope.dependents(),
                scope.assignedCourses()
        ).orElseThrow(() -> new ApiException(TrainingErrorCode.STUDENT_ENROLLMENT_NOT_FOUND));
    }

    private CourseSchedule resolveApplicableSchedule(StudentEnrollment enrollment, UUID scheduleId) {
        CourseSchedule schedule = courseScheduleRepository.findDetailedByIds(List.of(scheduleId)).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_SCHEDULE_NOT_FOUND));
        UUID enrollmentCourseId = enrollment.getCoursePurchase().getHomeCourse().getCourseId();
        if (!schedule.getCourse().getCourseId().equals(enrollmentCourseId)
                || schedule.getStartDate().isAfter(enrollment.getEndDate())
                || (schedule.getEndDate() != null && schedule.getEndDate().isBefore(enrollment.getStartDate()))) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT,
                    "Course schedule does not apply to enrollment period");
        }
        return schedule;
    }
}
