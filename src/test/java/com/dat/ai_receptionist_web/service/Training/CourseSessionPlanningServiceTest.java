package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLocation;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.LeaveRequestRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseSessionPlanningServiceTest {
    private static final LocalDate TODAY = LocalDate.now();

    private CourseRepository courseRepository;
    private CourseScheduleRepository courseScheduleRepository;
    private ClassSessionRepository classSessionRepository;
    private LeaveRequestRepository leaveRequestRepository;
    private CourseSessionPlanningService service;

    private ClassSchedule scheduleA;
    private CourseSchedule courseScheduleA;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        courseScheduleRepository = mock(CourseScheduleRepository.class);
        classSessionRepository = mock(ClassSessionRepository.class);
        leaveRequestRepository = mock(LeaveRequestRepository.class);

        service = new CourseSessionPlanningService(
                courseRepository, courseScheduleRepository, classSessionRepository,
                leaveRequestRepository);

        Branch branch = Branch.builder().branchId(1L).build();
        scheduleA = ClassSchedule.builder().scheduleId(UUID.randomUUID())
                .branch(branch).level(ScheduleLevel.BASIC).location(ScheduleLocation.INDOOR)
                .weekday(Weekday.MONDAY).startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30)).status(ScheduleStatus.ACTIVE).build();

        courseScheduleA = CourseSchedule.builder()
                .courseScheduleId(UUID.randomUUID())
                .classSchedule(scheduleA)
                .startDate(TODAY.minusDays(10))
                .status(ScheduleStatus.ACTIVE)
                .build();

        when(classSessionRepository.saveAll(any())).thenAnswer(invocation -> {
            List<ClassSession> sessions = invocation.getArgument(0);
            sessions.forEach(session -> session.setClassSessionId(UUID.randomUUID()));
            return sessions;
        });
    }

    @Test
    void maintainGenerationHorizonGeneratesSessionsForActiveCourses() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .courseId(courseId)
                .status(CourseStatus.ACTIVE)
                .classSessionGeneratedUntil(TODAY.plusDays(10))
                .build();
        courseScheduleA.setCourse(course);

        when(courseRepository.findCoursesNeedClassSessionGeneration(eq(CourseStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(course));
        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(courseScheduleRepository.findDetailedByCourseId(courseId)).thenReturn(List.of(courseScheduleA));
        when(classSessionRepository.findSessionDatesByCourseAndRange(any(), any(), any(), any()))
                .thenReturn(List.of());

        service.maintainGenerationHorizon();

        verify(courseRepository).save(argThat(saved ->
                saved.getClassSessionGeneratedUntil() != null
                        && saved.getClassSessionGeneratedUntil().equals(
                                TODAY.plusDays(CourseSessionPlanningService.CLASS_SESSION_GENERATION_HORIZON_DAYS))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClassSession>> captor = ArgumentCaptor.forClass(List.class);
        verify(classSessionRepository).saveAll(captor.capture());
        List<ClassSession> generated = captor.getValue();
        assertThat(generated).isNotEmpty();
        assertThat(generated).allSatisfy(session -> {
            assertThat(session.getCourse()).isEqualTo(course);
            assertThat(session.getCourseSchedule()).isEqualTo(courseScheduleA);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
            assertThat(session.isAttendanceClosed()).isFalse();
            assertThat(session.getStartTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(session.getEndTime()).isEqualTo(LocalTime.of(19, 30));
        });
    }
}
