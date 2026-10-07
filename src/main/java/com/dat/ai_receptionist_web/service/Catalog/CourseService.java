package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final ClassScheduleRepository classScheduleRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper courseScheduleMapper;
    private final CourseSessionPlanningService planningService;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseDTO.SimpleResponse> list(Pageable pageable) {
        Page<Course> courses = repository.findAllDetailed(pageable);
        List<UUID> courseIds = courses.getContent().stream()
                .map(Course::getCourseId)
                .toList();
        LocalDate currentDate = LocalDate.now();
        Map<UUID, CourseStaffView> staffByCourseId = getStaffByCourseIds(
                Set.copyOf(courseIds),
                currentDate);
        Map<UUID, Integer> currentStudentCountByCourseId =
                getCurrentStudentCountByCourseIds(courseIds, currentDate);
        return PageResponse.of(courses, course -> mapper.withSchedules(mapper.toSimpleResponse(
                        course,
                        staffByCourseId.getOrDefault(course.getCourseId(), CourseStaffView.empty()).primaryCoach(),
                        currentStudentCountByCourseId.getOrDefault(course.getCourseId(), 0)),
                courseScheduleRepository.findDetailedByCourseId(course.getCourseId()).stream()
                        .map(courseScheduleMapper::toSimpleResponse)
                        .toList()));
    }

    @Transactional(readOnly = true)
    public CourseDTO.Response get(UUID id) {
        Course course = find(id);
        return toResponseWithStaff(course);
    }

    @Transactional
    public CourseDTO.Response create(CourseDTO.CreateRequest request) {
        Course entity = new Course();
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        entity.setName(request.name());
        Course saved = repository.save(entity);
        replaceSchedules(saved, request.courseSchedules());
        if (saved.getStatus() == CourseStatus.ACTIVE) {
            planningService.maintainGenerationHorizon();
        }
        return toResponseWithStaff(saved);
    }

    @Transactional
    public CourseDTO.Response update(UUID id, CourseDTO.UpdateRequest request) {
        Course entity = find(id);
        entity.setName(request.name());
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        Course saved = repository.save(entity);
        if (saved.getStatus() == CourseStatus.ACTIVE) {
            planningService.maintainGenerationHorizon();
        }
        return toResponseWithStaff(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Course entity = find(id);
        entity.setStatus(CourseStatus.CANCELLED);
    }

    @Transactional
    public com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.Response addSchedule(
            UUID courseId, com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.UpsertRequest request) {
        Course course = find(courseId);
        CourseSchedule schedule = buildCourseSchedule(course, request);
        return courseScheduleMapper.toResponse(courseScheduleRepository.save(schedule));
    }

    @Transactional
    public com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.Response updateSchedule(
            UUID courseId, UUID id, com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.UpsertRequest request) {
        CourseSchedule existing = courseScheduleRepository.findById(id)
                .filter(item -> item.getCourse().getCourseId().equals(courseId))
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));
        CourseSchedule candidate = buildCourseSchedule(existing.getCourse(), request);
        existing.setClassSchedule(candidate.getClassSchedule());
        existing.setStartDate(candidate.getStartDate());
        existing.setEndDate(candidate.getEndDate());
        existing.setStatus(candidate.getStatus());
        return courseScheduleMapper.toResponse(courseScheduleRepository.save(existing));
    }

    @Transactional
    public void deactivateSchedule(UUID courseId, UUID id) {
        CourseSchedule schedule = courseScheduleRepository.findById(id)
                .filter(item -> item.getCourse().getCourseId().equals(courseId))
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));
        schedule.setStatus(com.dat.ai_receptionist_web.enums.Core.ScheduleStatus.INACTIVE);
    }

    private Course find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
    }

    private CourseDTO.Response toResponseWithStaff(Course course) {
        LocalDate currentDate = LocalDate.now();
        CourseStaffView staff = getStaffByCourseIds(Set.of(course.getCourseId()), currentDate)
                .getOrDefault(course.getCourseId(), CourseStaffView.empty());
        int currentStudentCount = Math.toIntExact(studentEnrollmentRepository.countCurrentStudentsByCourseId(
                course.getCourseId(),
                currentDate));
        return mapper.withSchedules(mapper.toResponse(
                course,
                staff.primaryCoach(),
                staff.manager(),
                currentStudentCount), courseScheduleRepository.findDetailedByCourseId(course.getCourseId()).stream()
                .map(courseScheduleMapper::toResponse).toList());
    }

    private void replaceSchedules(Course course, List<com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.UpsertRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT, "At least one course schedule is required");
        }
        for (var request : requests) {
            courseScheduleRepository.save(buildCourseSchedule(course, request));
        }
    }

    private CourseSchedule buildCourseSchedule(Course course, com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.UpsertRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ApiException(CatalogErrorCode.COURSE_SCHEDULE_CHANGE_CONFLICT, "Schedule end date must not precede start date");
        }
        var schedule = classScheduleRepository.findById(request.classScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND));
        return CourseSchedule.builder().course(course).classSchedule(schedule)
                .startDate(request.startDate()).endDate(request.endDate()).status(request.status()).build();
    }

    private Map<UUID, CourseStaffView> getStaffByCourseIds(Set<UUID> courseIds,
                                                           LocalDate effectiveDate) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<CourseStaffAssignment> assignments =
                courseStaffAssignmentRepository.findEffectiveStaffByCourseIdsAndDate(
                        courseIds,
                        EnumSet.allOf(AssignmentType.class),
                        effectiveDate);

        Map<UUID, CourseStaffViewBuilder> builders = new LinkedHashMap<>();
        for (CourseStaffAssignment assignment : assignments) {
            UUID courseId = assignment.getCourseSchedule().getCourse().getCourseId();
            CourseStaffViewBuilder builder = builders.computeIfAbsent(courseId, ignored -> new CourseStaffViewBuilder());
            builder.add(assignment);
        }

        return builders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().build(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<UUID, Integer> getCurrentStudentCountByCourseIds(List<UUID> courseIds,
                                                                 LocalDate currentDate) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return studentEnrollmentRepository.countCurrentStudentsByCourseIds(courseIds, currentDate).stream()
                .collect(Collectors.toMap(
                        StudentEnrollmentRepository.CourseStudentCount::getCourseId,
                        count -> Math.toIntExact(count.getStudentCount()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private record CourseStaffView(
            Person primaryCoach,
            List<Person> assistantCoaches,
            List<Person> teachingAssistants,
            Person manager
    ) {
        private static CourseStaffView empty() {
            return new CourseStaffView(null, List.of(), List.of(), null);
        }
    }

    private static final class CourseStaffViewBuilder {
        private Person primaryCoach;
        private final List<Person> assistantCoaches = new ArrayList<>();
        private final List<Person> teachingAssistants = new ArrayList<>();
        private Person manager;

        private void add(CourseStaffAssignment assignment) {
            switch (assignment.getAssignmentType()) {
                case PRIMARY_COACH -> {
                    if (primaryCoach == null) {
                        primaryCoach = assignment.getStaffPerson();
                    }
                }
                case ASSISTANT_COACH -> assistantCoaches.add(assignment.getStaffPerson());
                case TEACHING_ASSISTANT -> teachingAssistants.add(assignment.getStaffPerson());
                case MANAGER -> {
                    if (manager == null) {
                        manager = assignment.getStaffPerson();
                    }
                }
            }
        }

        private CourseStaffView build() {
            return new CourseStaffView(
                    primaryCoach,
                    List.copyOf(assistantCoaches),
                    List.copyOf(teachingAssistants),
                    manager);
        }
    }
}
