package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.enums.Training.AssignmentType;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseScheduleMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentScheduleRepository;
import com.dat.ai_receptionist_web.service.Training.CourseStaffAssignmentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                assignmentService,
                mock(ClassSessionRepository.class),
                mock(CourseStaffAssignmentRepository.class),
                mock(StudentEnrollmentScheduleRepository.class)
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

    @Test
    void deletesUnusedScheduleWhenCourseRetainsAnotherSchedule() {
        CourseScheduleRepository repository = mock(CourseScheduleRepository.class);
        ClassSessionRepository sessions = mock(ClassSessionRepository.class);
        CourseStaffAssignmentRepository assignments = mock(CourseStaffAssignmentRepository.class);
        StudentEnrollmentScheduleRepository enrollments = mock(StudentEnrollmentScheduleRepository.class);
        CourseScheduleService service = service(repository, sessions, assignments, enrollments);
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(Course.builder().courseId(courseId).build())
                .build();
        when(repository.findDetailedById(scheduleId)).thenReturn(Optional.of(schedule));
        when(repository.countByCourse_CourseId(courseId)).thenReturn(2L);

        service.delete(scheduleId);

        verify(repository).delete(schedule);
    }

    @Test
    void refusesToDeleteLastScheduleOrScheduleWithReferences() {
        CourseScheduleRepository repository = mock(CourseScheduleRepository.class);
        ClassSessionRepository sessions = mock(ClassSessionRepository.class);
        CourseStaffAssignmentRepository assignments = mock(CourseStaffAssignmentRepository.class);
        StudentEnrollmentScheduleRepository enrollments = mock(StudentEnrollmentScheduleRepository.class);
        CourseScheduleService service = service(repository, sessions, assignments, enrollments);
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(Course.builder().courseId(courseId).build())
                .build();
        when(repository.findDetailedById(scheduleId)).thenReturn(Optional.of(schedule));
        when(repository.countByCourse_CourseId(courseId)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(scheduleId)).isInstanceOf(RuntimeException.class);

        when(repository.countByCourse_CourseId(courseId)).thenReturn(2L);
        when(sessions.existsByCourseSchedule_CourseScheduleId(scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(scheduleId)).isInstanceOf(RuntimeException.class);
        verify(repository, never()).delete(schedule);
    }

    @Test
    void refusesToDeleteScheduleReferencedByAssignmentsOrEnrollments() {
        CourseScheduleRepository repository = mock(CourseScheduleRepository.class);
        ClassSessionRepository sessions = mock(ClassSessionRepository.class);
        CourseStaffAssignmentRepository assignments = mock(CourseStaffAssignmentRepository.class);
        StudentEnrollmentScheduleRepository enrollments = mock(StudentEnrollmentScheduleRepository.class);
        CourseScheduleService service = service(repository, sessions, assignments, enrollments);
        UUID courseId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(scheduleId)
                .course(Course.builder().courseId(courseId).build())
                .build();
        when(repository.findDetailedById(scheduleId)).thenReturn(Optional.of(schedule));
        when(repository.countByCourse_CourseId(courseId)).thenReturn(2L);
        when(assignments.existsByCourseSchedule_CourseScheduleId(scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(scheduleId)).isInstanceOf(RuntimeException.class);

        when(assignments.existsByCourseSchedule_CourseScheduleId(scheduleId)).thenReturn(false);
        when(enrollments.existsByCourseSchedule_CourseScheduleId(scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(scheduleId)).isInstanceOf(RuntimeException.class);
        verify(repository, never()).delete(schedule);
    }

    @Test
    void defaultsInactiveEndDateToTodayOrStartDate() {
        CourseScheduleRepository repository = mock(CourseScheduleRepository.class);
        ClassScheduleRepository classSchedules = mock(ClassScheduleRepository.class);
        CourseStaffAssignmentService assignmentService = mock(CourseStaffAssignmentService.class);
        CourseScheduleService service = new CourseScheduleService(
                repository, mock(CourseRepository.class), classSchedules, mock(CourseScheduleMapper.class),
                assignmentService, mock(ClassSessionRepository.class),
                mock(CourseStaffAssignmentRepository.class), mock(StudentEnrollmentScheduleRepository.class));
        UUID id = UUID.randomUUID();
        CourseSchedule schedule = CourseSchedule.builder().courseScheduleId(id).status(ScheduleStatus.ACTIVE).build();
        when(repository.findDetailedById(id)).thenReturn(Optional.of(schedule));
        when(classSchedules.findById(any())).thenReturn(Optional.of(new ClassSchedule()));
        when(repository.save(schedule)).thenReturn(schedule);
        when(assignmentService.getEffectiveBriefByCourseScheduleIds(any(), any())).thenReturn(Map.of());

        LocalDate futureStart = LocalDate.now().plusDays(3);
        service.update(id, new CourseScheduleDTO.UpdateRequest(UUID.randomUUID(), futureStart, null, ScheduleStatus.INACTIVE));

        assertThat(schedule.getEndDate()).isEqualTo(futureStart);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.INACTIVE);
    }

    @Test
    void clearsEndDateWhenReactivatingAnInactiveSchedule() {
        CourseScheduleRepository repository = mock(CourseScheduleRepository.class);
        ClassScheduleRepository classSchedules = mock(ClassScheduleRepository.class);
        CourseStaffAssignmentService assignmentService = mock(CourseStaffAssignmentService.class);
        CourseScheduleService service = new CourseScheduleService(
                repository, mock(CourseRepository.class), classSchedules, mock(CourseScheduleMapper.class),
                assignmentService, mock(ClassSessionRepository.class),
                mock(CourseStaffAssignmentRepository.class), mock(StudentEnrollmentScheduleRepository.class));
        UUID id = UUID.randomUUID();
        CourseSchedule schedule = CourseSchedule.builder()
                .courseScheduleId(id)
                .status(ScheduleStatus.INACTIVE)
                .endDate(LocalDate.now())
                .build();
        when(repository.findDetailedById(id)).thenReturn(Optional.of(schedule));
        when(classSchedules.findById(any())).thenReturn(Optional.of(new ClassSchedule()));
        when(repository.save(schedule)).thenReturn(schedule);
        when(assignmentService.getEffectiveBriefByCourseScheduleIds(any(), any())).thenReturn(Map.of());

        service.update(id, new CourseScheduleDTO.UpdateRequest(
                UUID.randomUUID(), LocalDate.now().minusDays(1), LocalDate.now(), ScheduleStatus.ACTIVE));

        assertThat(schedule.getEndDate()).isNull();
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
    }

    private CourseScheduleService service(
            CourseScheduleRepository repository,
            ClassSessionRepository sessions,
            CourseStaffAssignmentRepository assignments,
            StudentEnrollmentScheduleRepository enrollments
    ) {
        return new CourseScheduleService(
                repository,
                mock(CourseRepository.class),
                mock(ClassScheduleRepository.class),
                mock(CourseScheduleMapper.class),
                mock(CourseStaffAssignmentService.class),
                sessions,
                assignments,
                enrollments
        );
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
