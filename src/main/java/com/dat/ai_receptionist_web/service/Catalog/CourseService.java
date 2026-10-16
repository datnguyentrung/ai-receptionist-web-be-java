package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final CourseScheduleService courseScheduleService;
    private final CourseSessionPlanningService planningService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final ClassSessionRepository classSessionRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseDTO.SimpleResponse> list(Pageable pageable) {
        Page<Course> courses = repository.findAll(pageable);
        List<UUID> courseIds = courses.getContent().stream()
                .map(Course::getCourseId)
                .toList();
        LocalDate currentDate = LocalDate.now();
        Map<UUID, Integer> currentStudentCountByCourseId =
                getCurrentStudentCountByCourseIds(courseIds, currentDate);
        Map<UUID, List<CourseScheduleDTO.SimpleResponse>> schedulesByCourseId =
                courseScheduleService.findSimpleResponsesByCourseIds(courseIds);
        return PageResponse.of(courses, course -> mapper.withSchedules(mapper.toSimpleResponse(
                        course,
                        currentStudentCountByCourseId.getOrDefault(course.getCourseId(), 0)),
                schedulesByCourseId.getOrDefault(course.getCourseId(), List.of())));
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
        entity.setLevel(request.level());
        Course saved = repository.save(entity);
        courseScheduleService.createForCourse(saved, request.courseSchedules());
        if (saved.getStatus() == CourseStatus.ACTIVE) {
            planningService.maintainGenerationHorizon();
        }
        return toResponseWithStaff(saved);
    }

    @Transactional
    public CourseDTO.Response update(UUID id, CourseDTO.UpdateRequest request) {
        Course entity = find(id);
        ScheduleLevel previousLevel = entity.getLevel();
        entity.setName(request.name());
        entity.setLevel(request.level());
        entity.setCapacity(request.capacity());
        entity.setStatus(request.status());
        Course saved = repository.save(entity);
        if (previousLevel != saved.getLevel()) {
            classSessionRepository.updateFutureScheduleLevel(
                    saved.getCourseId(), LocalDate.now(), saved.getLevel().name());
        }
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

    private Course find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
    }

    private CourseDTO.Response toResponseWithStaff(Course course) {
        LocalDate currentDate = LocalDate.now();
        int currentStudentCount = Math.toIntExact(studentEnrollmentRepository.countCurrentStudentsByCourseId(
                course.getCourseId(),
                currentDate));
        return mapper.withSchedules(mapper.toResponse(
                course,
                currentStudentCount), courseScheduleService.findResponsesByCourseId(course.getCourseId()));
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

}
