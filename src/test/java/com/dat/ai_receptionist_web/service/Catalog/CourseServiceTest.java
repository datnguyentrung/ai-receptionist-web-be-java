package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.enums.Training.CourseStaffAssignmentStatus;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourseServiceTest {
    @Test
    void listMapsPrimaryCoachUsingOneBatchStaffAssignmentQuery() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseMapper mapper = mock(CourseMapper.class);
        CourseStaffAssignmentRepository assignmentRepository = mock(CourseStaffAssignmentRepository.class);
        CourseSessionPlanningService planningService = mock(CourseSessionPlanningService.class);
        Pageable pageable = Pageable.unpaged();
        Course course = Course.builder()
                .courseId(UUID.randomUUID())
                .name("Course A")
                .status(CourseStatus.ACTIVE)
                .build();
        Person coach = Person.builder()
                .personId(UUID.randomUUID())
                .fullName("Coach A")
                .build();
        CourseStaffAssignment assignment = CourseStaffAssignment.builder()
                .course(course)
                .staffPerson(coach)
                .assignmentType(AssignmentType.PRIMARY_COACH)
                .assignmentStatus(CourseStaffAssignmentStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(1))
                .build();
        CourseDTO.SimpleResponse response = new CourseDTO.SimpleResponse(
                course.getCourseId(),
                null,
                null,
                null,
                course.getName(),
                0,
                CourseStatus.ACTIVE,
                null);

        when(repository.findAllDetailed(pageable)).thenReturn(new PageImpl<>(List.of(course)));
        when(assignmentRepository.findEffectiveStaffByCourseIdsAndDate(
                anyCollection(),
                anyCollection(),
                eq(LocalDate.now()))).thenReturn(List.of(assignment));
        when(mapper.toSimpleResponse(course, coach)).thenReturn(response);

        CourseService service = new CourseService(
                repository,
                mapper,
                mock(ClassScheduleRepository.class),
                planningService,
                assignmentRepository);

        var result = service.list(pageable);

        assertThat(result.getContent()).containsExactly(response);
        verify(assignmentRepository).findEffectiveStaffByCourseIdsAndDate(
                argThat(ids -> ids.contains(course.getCourseId()) && ids.size() == 1),
                anyCollection(),
                eq(LocalDate.now()));
        verify(mapper).toSimpleResponse(course, coach);
    }
}
