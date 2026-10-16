package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourseServiceTest {
    @Test
    void updateLevelRefreshesOnlyEligibleFutureSessionSnapshots() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseMapper mapper = mock(CourseMapper.class);
        StudentEnrollmentRepository enrollmentRepository = mock(StudentEnrollmentRepository.class);
        ClassSessionRepository classSessionRepository = mock(ClassSessionRepository.class);
        CourseScheduleService courseScheduleService = mock(CourseScheduleService.class);
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .courseId(courseId)
                .name("Course A")
                .level(ScheduleLevel.BASIC)
                .capacity(20)
                .status(CourseStatus.CLOSED)
                .build();
        CourseDTO.Response response = new CourseDTO.Response(
                courseId, List.of(), "Course A", ScheduleLevel.ADVANCED,
                20, 0, CourseStatus.CLOSED, null, null, null);

        when(repository.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(repository.save(course)).thenReturn(course);
        when(mapper.toResponse(course, 0)).thenReturn(response);
        when(mapper.withSchedules(response, List.of())).thenReturn(response);
        when(courseScheduleService.findResponsesByCourseId(courseId)).thenReturn(List.of());

        CourseService service = new CourseService(
                repository,
                mapper,
                courseScheduleService,
                mock(CourseSessionPlanningService.class),
                enrollmentRepository,
                classSessionRepository);

        CourseDTO.Response result = service.update(courseId,
                new CourseDTO.UpdateRequest("Course A", ScheduleLevel.ADVANCED, 20, CourseStatus.CLOSED));

        assertThat(result).isSameAs(response);
        verify(classSessionRepository).updateFutureScheduleLevel(
                courseId, LocalDate.now(), ScheduleLevel.ADVANCED.name());
    }

    @Test
    void listMapsSchedulesAndStudentCountsWithoutLoadingStaffAssignments() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseMapper mapper = mock(CourseMapper.class);
        StudentEnrollmentRepository studentEnrollmentRepository = mock(StudentEnrollmentRepository.class);
        CourseSessionPlanningService planningService = mock(CourseSessionPlanningService.class);
        CourseScheduleService courseScheduleService = mock(CourseScheduleService.class);
        Pageable pageable = Pageable.unpaged();
        Course course = Course.builder()
                .courseId(UUID.randomUUID())
                .name("Course A")
                .status(CourseStatus.ACTIVE)
                .build();
        CourseDTO.SimpleResponse response = new CourseDTO.SimpleResponse(
                course.getCourseId(),
                List.of(),
                course.getName(),
                ScheduleLevel.BASIC,
                0,
                3,
                CourseStatus.ACTIVE);

        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(course)));
        when(studentEnrollmentRepository.countCurrentStudentsByCourseIds(
                anyList(),
                eq(LocalDate.now()))).thenReturn(List.of(new CourseStudentCountRow(course.getCourseId(), 3)));
        when(mapper.toSimpleResponse(course, 3)).thenReturn(response);
        when(mapper.withSchedules(eq(response), anyList())).thenReturn(response);
        when(courseScheduleService.findSimpleResponsesByCourseIds(anyList())).thenReturn(Map.of());

        CourseService service = new CourseService(
                repository,
                mapper,
                courseScheduleService,
                planningService,
                studentEnrollmentRepository,
                mock(com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository.class));

        var result = service.list(pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(studentEnrollmentRepository).countCurrentStudentsByCourseIds(
                argThat(ids -> ids.contains(course.getCourseId()) && ids.size() == 1),
                eq(LocalDate.now()));
        verify(mapper).toSimpleResponse(course, 3);
        verify(mapper).withSchedules(eq(response), anyList());
        verify(courseScheduleService).findSimpleResponsesByCourseIds(List.of(course.getCourseId()));
    }

    private record CourseStudentCountRow(UUID courseId, long studentCount)
            implements StudentEnrollmentRepository.CourseStudentCount {
        @Override
        public UUID getCourseId() {
            return courseId;
        }

        @Override
        public long getStudentCount() {
            return studentCount;
        }
    }
}
