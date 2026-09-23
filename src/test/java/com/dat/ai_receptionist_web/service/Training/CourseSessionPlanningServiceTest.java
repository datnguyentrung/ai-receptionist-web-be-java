package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.LeaveRequest;
import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Core.Weekday;
import com.dat.ai_receptionist_web.enums.Training.LeaveRequestStatus;
import com.dat.ai_receptionist_web.enums.Training.ScheduleImpactType;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.repository.Catalog.ClassScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.LeaveRequestRepository;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseScheduleChangeNotifier;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseSessionPlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseSessionPlanningServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 30); // Sunday

    private CourseRepository courseRepository;
    private ClassScheduleRepository classScheduleRepository;
    private ClassSessionRepository classSessionRepository;
    private LeaveRequestRepository leaveRequestRepository;
    private CourseScheduleChangeNotifier changeNotifier;
    private CourseMapper courseMapper;
    private CourseSessionPlanningService service;

    private ClassSchedule scheduleA;
    private ClassSchedule scheduleB;

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        classScheduleRepository = mock(ClassScheduleRepository.class);
        classSessionRepository = mock(ClassSessionRepository.class);
        leaveRequestRepository = mock(LeaveRequestRepository.class);
        changeNotifier = mock(CourseScheduleChangeNotifier.class);
        courseMapper = mock(CourseMapper.class, CALLS_REAL_METHODS);

        service = new CourseSessionPlanningService(
                courseRepository, classScheduleRepository, classSessionRepository,
                leaveRequestRepository, changeNotifier, courseMapper);

        scheduleA = ClassSchedule.builder().scheduleId(UUID.randomUUID())
                .weekday(Weekday.MONDAY).startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30)).status(ScheduleStatus.ACTIVE).build();
        scheduleB = ClassSchedule.builder().scheduleId(UUID.randomUUID())
                .weekday(Weekday.WEDNESDAY).startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30)).status(ScheduleStatus.ACTIVE).build();

        when(classSessionRepository.saveAll(any())).thenAnswer(invocation -> {
            List<ClassSession> sessions = invocation.getArgument(0);
            sessions.forEach(session -> session.setClassSessionId(UUID.randomUUID()));
            return sessions;
        });
    }

    @Test
    void immediateChangeCancelsFutureSessionsAndGeneratesUnderNewSchedule() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().courseId(courseId).classSchedule(scheduleA)
                .status(CourseStatus.ACTIVE).build();
        ClassSession upcoming = ClassSession.builder().classSessionId(UUID.randomUUID())
                .course(course).sessionDate(TODAY.plusDays(1))
                .status(SessionStatus.SCHEDULED).startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30)).build();

        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(classScheduleRepository.findById(scheduleB.getScheduleId()))
                .thenReturn(Optional.of(scheduleB));
        when(classSessionRepository.findUpcomingSessionsToCancel(any(), any(), any(), any()))
                .thenReturn(List.of(upcoming));
        when(classSessionRepository.findSessionDatesByCourseAndRange(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByReferencedSessionIds(any())).thenReturn(List.of());
        when(courseMapper.toResponse(any())).thenReturn(stubResponse(course));

        CourseDTO.CourseScheduleChangeResponse result = service.changeSchedule(
                courseId, scheduleB.getScheduleId(), TODAY);

        assertThat(result.cancelledSessionIds()).containsExactly(upcoming.getClassSessionId());
        assertThat(upcoming.getStatus()).isEqualTo(SessionStatus.CANCELLED);
        assertThat(course.getClassSchedule()).isEqualTo(scheduleB);
        assertThat(course.getNextClassSchedule()).isNull();
        assertThat(course.getNextScheduleEffectiveFrom()).isNull();
        assertThat(course.getClassSessionGeneratedUntil())
                .isEqualTo(LocalDate.now()
                        .plusDays(CourseSessionPlanningService.CLASS_SESSION_GENERATION_HORIZON_DAYS));

        ArgumentCaptor<List<ClassSession>> captor = ArgumentCaptor.forClass(List.class);
        verify(classSessionRepository).saveAll(captor.capture());
        List<ClassSession> generated = captor.getValue();
        assertThat(generated).isNotEmpty();
        assertThat(generated).allSatisfy(session -> {
            assertThat(session.getSessionDate().getDayOfWeek().getValue()).isEqualTo(3); // WEDNESDAY
            assertThat(session.getStartTime()).isEqualTo(LocalTime.of(19, 0));
            assertThat(session.getEndTime()).isEqualTo(LocalTime.of(20, 30));
            assertThat(session.getStatus()).isEqualTo(SessionStatus.SCHEDULED);
            assertThat(session.isAttendanceClosed()).isFalse();
        });
        verify(changeNotifier).notifyAfterCommit(courseId, List.of());
    }

    @Test
    void immediateChangeReportsAffectedLeaveRequestsInMemoryOnly() {
        UUID courseId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Course course = Course.builder().courseId(courseId).classSchedule(scheduleA)
                .status(CourseStatus.ACTIVE).build();
        ClassSession upcoming = ClassSession.builder().classSessionId(UUID.randomUUID())
                .course(course).sessionDate(TODAY.plusDays(1))
                .status(SessionStatus.SCHEDULED).startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30)).build();
        LeaveRequest request = LeaveRequest.builder().leaveRequestId(UUID.randomUUID())
                .person(Person.builder().personId(personId).build())
                .leaveClassSession(upcoming)
                .status(LeaveRequestStatus.PENDING).build();

        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(classScheduleRepository.findById(scheduleB.getScheduleId()))
                .thenReturn(Optional.of(scheduleB));
        when(classSessionRepository.findUpcomingSessionsToCancel(any(), any(), any(), any()))
                .thenReturn(List.of(upcoming));
        when(classSessionRepository.findSessionDatesByCourseAndRange(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByReferencedSessionIds(
                List.of(upcoming.getClassSessionId()))).thenReturn(List.of(request));
        when(courseMapper.toResponse(any())).thenReturn(stubResponse(course));

        service.changeSchedule(courseId, scheduleB.getScheduleId(), TODAY);

        ArgumentCaptor<List<CourseScheduleChangeNotifier.AffectedLeaveRequest>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(changeNotifier).notifyAfterCommit(eq(courseId), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        CourseScheduleChangeNotifier.AffectedLeaveRequest affected = captor.getValue().get(0);
        assertThat(affected.leaveRequestId()).isEqualTo(request.getLeaveRequestId());
        assertThat(affected.personId()).isEqualTo(personId);
        assertThat(affected.courseId()).isEqualTo(courseId);
        assertThat(affected.classSessionId()).isEqualTo(upcoming.getClassSessionId());
        assertThat(affected.impactType()).isEqualTo(ScheduleImpactType.LEAVE_SESSION);
        assertThat(affected.requestStatus()).isEqualTo(LeaveRequestStatus.PENDING);
    }

    @Test
    void futureChangeKeepsCurrentScheduleAndPreparesPendingPlan() {
        UUID courseId = UUID.randomUUID();
        LocalDate effectiveFrom = TODAY.plusDays(30);
        Course course = Course.builder().courseId(courseId).classSchedule(scheduleA)
                .status(CourseStatus.ACTIVE).classSessionGeneratedUntil(effectiveFrom.minusDays(1))
                .build();

        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(classScheduleRepository.findById(scheduleB.getScheduleId()))
                .thenReturn(Optional.of(scheduleB));
        when(classSessionRepository.findUpcomingSessionsToCancel(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(classSessionRepository.findSessionDatesByCourseAndRange(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByReferencedSessionIds(any())).thenReturn(List.of());
        when(courseMapper.toResponse(any())).thenReturn(stubResponse(course));

        CourseDTO.CourseScheduleChangeResponse result = service.changeSchedule(
                courseId, scheduleB.getScheduleId(), effectiveFrom);

        assertThat(course.getClassSchedule()).isEqualTo(scheduleA);
        assertThat(course.getNextClassSchedule()).isEqualTo(scheduleB);
        assertThat(course.getNextScheduleEffectiveFrom()).isEqualTo(effectiveFrom);
        assertThat(result.cancelledSessionIds()).isEmpty();

        ArgumentCaptor<List<ClassSession>> captor = ArgumentCaptor.forClass(List.class);
        verify(classSessionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(session ->
                assertThat(session.getSessionDate()).isAfterOrEqualTo(effectiveFrom));
        verify(changeNotifier).notifyAfterCommit(courseId, List.of());
    }

    @Test
    void repeatedIdenticalPendingChangeIsANoOp() {
        UUID courseId = UUID.randomUUID();
        LocalDate effectiveFrom = TODAY.plusDays(30);
        Course course = Course.builder().courseId(courseId).classSchedule(scheduleA)
                .nextClassSchedule(scheduleB).nextScheduleEffectiveFrom(effectiveFrom)
                .status(CourseStatus.ACTIVE).build();

        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(classScheduleRepository.findById(scheduleB.getScheduleId()))
                .thenReturn(Optional.of(scheduleB));
        when(courseMapper.toResponse(any())).thenReturn(stubResponse(course));

        CourseDTO.CourseScheduleChangeResponse result = service.changeSchedule(
                courseId, scheduleB.getScheduleId(), effectiveFrom);

        assertThat(result.cancelledSessionIds()).isEmpty();
        assertThat(result.generatedSessionIds()).isEmpty();
        verify(classSessionRepository, never()).findUpcomingSessionsToCancel(any(), any(), any(), any());
        verify(changeNotifier, never()).notifyAfterCommit(any(), any());
    }

    @Test
    void changeToCurrentScheduleIsANoOp() {
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().courseId(courseId).classSchedule(scheduleA)
                .status(CourseStatus.ACTIVE).build();

        when(courseRepository.findByIdForUpdate(courseId)).thenReturn(Optional.of(course));
        when(classScheduleRepository.findById(scheduleA.getScheduleId()))
                .thenReturn(Optional.of(scheduleA));
        when(courseMapper.toResponse(any())).thenReturn(stubResponse(course));

        CourseDTO.CourseScheduleChangeResponse result = service.changeSchedule(
                courseId, scheduleA.getScheduleId(), TODAY);

        assertThat(result.cancelledSessionIds()).isEmpty();
        assertThat(result.generatedSessionIds()).isEmpty();
        verify(classSessionRepository, never()).findUpcomingSessionsToCancel(any(), any(), any(), any());
        verify(changeNotifier, never()).notifyAfterCommit(any(), any());
    }

    private CourseDTO.Response stubResponse(Course course) {
        ClassScheduleDTO.Response schedule = new ClassScheduleDTO.Response(
                scheduleA.getScheduleId(), null, scheduleA.getWeekday(), null, null,
                scheduleA.getStatus(), scheduleA.getStartTime(), scheduleA.getEndTime());
        return new CourseDTO.Response(
                course.getCourseId(), schedule, null, null,
                "Course A", 10, CourseStatus.ACTIVE, null,
                null, List.of(), List.of(), null,
                null, null);
    }
}
