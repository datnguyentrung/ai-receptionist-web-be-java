package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
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
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
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
    private final CourseSessionPlanningService planningService;
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseDTO.SimpleResponse> list(Pageable pageable) {
        Page<Course> courses = repository.findAllDetailed(pageable);
        Map<UUID, CourseStaffView> staffByCourseId = getStaffByCourseIds(
                courses.getContent().stream()
                        .map(Course::getCourseId)
                        .collect(Collectors.toSet()),
                LocalDate.now());
        return PageResponse.of(courses, course -> mapper.toSimpleResponse(
                course,
                staffByCourseId.getOrDefault(course.getCourseId(), CourseStaffView.empty()).primaryCoach()));
    }

    @Transactional(readOnly = true)
    public CourseDTO.Response get(UUID id) {
        Course course = find(id);
        return toResponseWithStaff(course);
    }

    @Transactional
    public CourseDTO.Response create(CourseDTO.CreateRequest request) {
        Course entity = new Course();
        entity.setClassSchedule(classScheduleRepository.findById(request.classScheduleId())
                .orElseThrow(() -> new ApiException(CatalogErrorCode.CLASS_SCHEDULE_NOT_FOUND)));
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        entity.setName(request.name());
        Course saved = repository.save(entity);
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
    public CourseDTO.CourseScheduleChangeResponse changeSchedule(
            UUID id, CourseDTO.ScheduleChangeRequest request) {
        CourseDTO.CourseScheduleChangeResponse result =
                planningService.changeSchedule(id, request.classScheduleId(), request.effectiveFrom());
        return new CourseDTO.CourseScheduleChangeResponse(
                get(id),
                result.cancelledSessionIds(),
                result.generatedSessionIds());
    }

    @Transactional
    public void cancelPendingScheduleChange(UUID id) {
        planningService.cancelPendingScheduleChange(id);
    }

    private Course find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
    }

    private CourseDTO.Response toResponseWithStaff(Course course) {
        CourseStaffView staff = getStaffByCourseIds(Set.of(course.getCourseId()), LocalDate.now())
                .getOrDefault(course.getCourseId(), CourseStaffView.empty());
        return mapper.toResponse(
                course,
                staff.primaryCoach(),
                staff.assistantCoaches(),
                staff.teachingAssistants(),
                staff.manager());
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
            UUID courseId = assignment.getCourse().getCourseId();
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
