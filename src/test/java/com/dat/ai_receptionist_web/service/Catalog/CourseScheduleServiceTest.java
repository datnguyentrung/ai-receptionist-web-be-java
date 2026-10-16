package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.service.Training.CourseStaffAssignmentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CourseScheduleServiceTest {
    @Test
    void attachesStaffToSchedulesUsingOneBatchLookup() {
        CourseScheduleRepository repository = mock(CourseScheduleRepository.class);
        CourseScheduleMapper mapper = mock(CourseScheduleMapper.class);
        CourseStaffAssignmentService assignmentService = mock(CourseStaffAssignmentService.class);
        CourseScheduleService service = new CourseScheduleService(
                repository,
                mock(CourseRepository.class),
                mock(ClassScheduleRepository.class),
                mapper,
                assignmentService
        );
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().courseId(courseId).build();
        CourseSchedule first = CourseSchedule.builder()
                .courseScheduleId(UUID.randomUUID()).course(course).build();
        CourseSchedule second = CourseSchedule.builder()
                .courseScheduleId(UUID.randomUUID()).course(course).build();
        var primary = new CourseStaffAssignmentDTO.BriefResponse(
                UUID.randomUUID(), null, AssignmentType.PRIMARY_COACH);
        CourseScheduleDTO.Response firstResponse = response(first, List.of(primary));
        CourseScheduleDTO.Response secondResponse = response(second, List.of());

        when(repository.findDetailedByCourseId(courseId)).thenReturn(List.of(first, second));
        when(assignmentService.getEffectiveBriefByCourseScheduleIds(any(), any()))
                .thenReturn(Map.of(first.getCourseScheduleId(), List.of(primary)));
        when(mapper.toResponse(first, List.of(primary))).thenReturn(firstResponse);
        when(mapper.toResponse(second, List.of())).thenReturn(secondResponse);

        var result = service.findResponsesByCourseId(courseId);

        assertThat(result).containsExactly(firstResponse, secondResponse);
        verify(assignmentService, times(1)).getEffectiveBriefByCourseScheduleIds(
                eq(List.of(first.getCourseScheduleId(), second.getCourseScheduleId())), any(LocalDate.class));
    }

    private CourseScheduleDTO.Response response(
            CourseSchedule schedule,
            List<CourseStaffAssignmentDTO.BriefResponse> staff
    ) {
        return new CourseScheduleDTO.Response(
                schedule.getCourseScheduleId(),
                schedule.getCourse().getCourseId(),
                null,
                staff,
                null,
                null,
                null,
                null,
                null
        );
    }
}
