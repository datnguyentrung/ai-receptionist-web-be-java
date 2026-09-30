package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.mapper.Training.ClassSessionMapper;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.CourseStaffAssignmentRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.access.ClassSessionAccessPolicy;
import com.dat.ai_receptionist_web.service.Training.access.TrainingAccessScope;
import com.dat.ai_receptionist_web.service.Training.session.ClassSessionService;
import com.dat.ai_receptionist_web.service.Training.scheduling.CourseScheduleResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ClassSessionServiceTest {
    private final ClassSessionRepository repository = mock(ClassSessionRepository.class);
    private final CourseStaffAssignmentRepository courseStaffAssignmentRepository = mock(CourseStaffAssignmentRepository.class);
    private final SessionAttendanceRepository sessionAttendanceRepository = mock(SessionAttendanceRepository.class);
    private final ClassSessionMapper mapper = mock(ClassSessionMapper.class);
    private final CurrentAccessContextResolver accessContextResolver = mock(CurrentAccessContextResolver.class);
    private final ClassSessionAccessPolicy accessPolicy = mock(ClassSessionAccessPolicy.class);
    private final ClassSessionService service = new ClassSessionService(
            repository,
            mapper,
            mock(CourseRepository.class),
            accessContextResolver,
            accessPolicy,
            courseStaffAssignmentRepository,
            new CourseScheduleResolver(),
            sessionAttendanceRepository
    );

    @Test
    void getIncludesRoundedLearningProgressForAccessibleCourse() {
        AccessContext context = unrestrictedContext();
        ClassSession session = session(
                LocalDate.of(2026, 9, 30), LocalTime.of(18, 0), "Course");
        ClassSessionRepository.LearningProgressRow row =
                mock(ClassSessionRepository.LearningProgressRow.class);
        ClassSessionDTO.Response response = new ClassSessionDTO.Response(
                session.getClassSessionId(),
                null,
                session.getSessionDate(),
                session.getStatus(),
                session.isAttendanceClosed(),
                null,
                session.getStartTime(),
                session.getEndTime(),
                null,
                new ClassSessionDTO.LearningProgress(12, 36, 33)
        );

        when(accessContextResolver.current()).thenReturn(context);
        when(accessPolicy.resolveReadScope(context))
                .thenReturn(new TrainingAccessScope(true, false, false, false, false));
        when(repository.findAccessibleById(
                session.getClassSessionId(), context.activePersonId(), true))
                .thenReturn(Optional.of(session));
        when(repository.findLearningProgressByCourseId(session.getCourse().getCourseId()))
                .thenReturn(row);
        when(row.getCompleted()).thenReturn(12L);
        when(row.getTotal()).thenReturn(36L);
        when(mapper.toResponse(eq(session), any(ClassSessionDTO.LearningProgress.class)))
                .thenReturn(response);

        assertThat(service.get(session.getClassSessionId())).isSameAs(response);
        verify(mapper).toResponse(eq(session), argThat(progress ->
                progress.completed() == 12
                        && progress.total() == 36
                        && progress.percent() == 33));
    }

    @Test
    void calendarReturnsSessionsInDateAndStartTimeOrder() {
        LocalDate fromDate = LocalDate.of(2026, 9, 14);
        LocalDate toDate = LocalDate.of(2026, 10, 4);
        AccessContext context = unrestrictedContext();
        ClassSession late = session(LocalDate.of(2026, 9, 16), LocalTime.of(19, 0), "Late");
        ClassSession early = session(LocalDate.of(2026, 9, 16), LocalTime.of(17, 30), "Early");
        ClassSession firstDay = session(LocalDate.of(2026, 9, 14), LocalTime.of(18, 0), "First");
        ClassSessionDTO.CalendarResponse lateResponse = response(late);
        ClassSessionDTO.CalendarResponse earlyResponse = response(early);
        ClassSessionDTO.CalendarResponse firstDayResponse = response(firstDay);

        when(accessContextResolver.current()).thenReturn(context);
        when(accessPolicy.resolveReadScope(context))
                .thenReturn(new TrainingAccessScope(true, false, false, false, false));
        when(repository.findCalendarSessions(fromDate, toDate, context.activePersonId(), true))
                .thenReturn(List.of(late, early, firstDay));
        when(courseStaffAssignmentRepository.findEffectivePrimaryCoachAssignmentsForCourseIdsBetween(anySet(), eq(fromDate), eq(toDate)))
                .thenReturn(List.of());
        when(mapper.toCalendarResponse(late, null)).thenReturn(lateResponse);
        when(mapper.toCalendarResponse(early, null)).thenReturn(earlyResponse);
        when(mapper.toCalendarResponse(firstDay, null)).thenReturn(firstDayResponse);

        List<ClassSessionDTO.CalendarResponse> result = service.getCalendar(fromDate, toDate);

        assertThat(result).containsExactly(firstDayResponse, earlyResponse, lateResponse);
    }

    @Test
    void evaluationReturnsActiveStudentsAndKeepsMissingAttendanceUnrecorded() {
        AccessContext context = unrestrictedContext();
        ClassSession session = session(
                LocalDate.of(2026, 9, 30), LocalTime.of(18, 0), "Course");
        ClassSessionDTO.SimpleResponse sessionResponse = new ClassSessionDTO.SimpleResponse(
                session.getClassSessionId(),
                null,
                session.getSessionDate(),
                session.getStatus(),
                session.isAttendanceClosed(),
                session.getStartTime(),
                session.getEndTime(),
                null
        );
        SessionAttendanceRepository.ClassSessionEvaluationRow recorded = evaluationRow(
                UUID.randomUUID(), "An Nguyen", UUID.randomUUID(),
                AttendanceStatus.LATE, EvaluationStatus.GOOD);
        SessionAttendanceRepository.ClassSessionEvaluationRow missing = evaluationRow(
                UUID.randomUUID(), "Binh Tran", null, null, null);

        when(accessContextResolver.current()).thenReturn(context);
        when(accessPolicy.resolveReadScope(context))
                .thenReturn(new TrainingAccessScope(true, false, false, false, false));
        when(repository.findAccessibleById(
                session.getClassSessionId(), context.activePersonId(), true))
                .thenReturn(Optional.of(session));
        when(sessionAttendanceRepository.findClassSessionEvaluationRows(
                session.getClassSessionId(),
                session.getCourse().getCourseId(),
                session.getSessionDate()))
                .thenReturn(List.of(recorded, missing));
        when(mapper.toSimpleResponse(session, null)).thenReturn(sessionResponse);

        ClassSessionDTO.EvaluationResponse result = service.getEvaluation(session.getClassSessionId());

        assertThat(result.classSession()).isSameAs(sessionResponse);
        assertThat(result.students()).hasSize(2);
        assertThat(result.students().getFirst().recorded()).isTrue();
        assertThat(result.students().getFirst().attendance().attendanceStatus())
                .isEqualTo(AttendanceStatus.LATE);
        assertThat(result.students().getFirst().attendance().evaluationStatus())
                .isEqualTo(EvaluationStatus.GOOD);
        assertThat(result.students().getLast().recorded()).isFalse();
        assertThat(result.students().getLast().attendance()).isNull();
    }

    @Test
    void calendarAcceptsSameDayRange() {
        LocalDate date = LocalDate.of(2026, 9, 14);
        AccessContext context = unrestrictedContext();
        ClassSession session = session(date, LocalTime.of(18, 0), "Course");
        ClassSessionDTO.CalendarResponse response = response(session);

        when(accessContextResolver.current()).thenReturn(context);
        when(accessPolicy.resolveReadScope(context))
                .thenReturn(new TrainingAccessScope(true, false, false, false, false));
        when(repository.findCalendarSessions(date, date, context.activePersonId(), true))
                .thenReturn(List.of(session));
        when(courseStaffAssignmentRepository.findEffectivePrimaryCoachAssignmentsForCourseIdsBetween(anySet(), eq(date), eq(date)))
                .thenReturn(List.of());
        when(mapper.toCalendarResponse(session, null)).thenReturn(response);

        assertThat(service.getCalendar(date, date)).containsExactly(response);
    }

    @Test
    void calendarReturnsEmptyListWhenNoSessionsExistInRange() {
        LocalDate fromDate = LocalDate.of(2026, 9, 14);
        LocalDate toDate = LocalDate.of(2026, 9, 20);
        AccessContext context = unrestrictedContext();

        when(accessContextResolver.current()).thenReturn(context);
        when(accessPolicy.resolveReadScope(context))
                .thenReturn(new TrainingAccessScope(true, false, false, false, false));
        when(repository.findCalendarSessions(fromDate, toDate, context.activePersonId(), true))
                .thenReturn(List.of());

        assertThat(service.getCalendar(fromDate, toDate)).isEmpty();
    }

    @Test
    void calendarRejectsInvertedDateRange() {
        assertThatThrownBy(() -> service.getCalendar(
                LocalDate.of(2026, 10, 4),
                LocalDate.of(2026, 9, 14)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(GeneralErrorCode.INVALID_REQUEST_PARAMETER));
        verifyNoInteractions(repository);
    }

    @Test
    void calendarRejectsRangeOverMaximum() {
        assertThatThrownBy(() -> service.getCalendar(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 14)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(GeneralErrorCode.INVALID_REQUEST_PARAMETER));
        verifyNoInteractions(repository);
    }

    private static AccessContext unrestrictedContext() {
        return new AccessContext(
                UUID.randomUUID(),
                null,
                null,
                null,
                Set.of(SystemRoleDefinition.SUPER_ADMIN.getCode()),
                Set.of()
        );
    }

    private static ClassSession session(LocalDate date, LocalTime startTime, String courseName) {
        Course course = Course.builder()
                .courseId(UUID.randomUUID())
                .name(courseName)
                .build();
        return ClassSession.builder()
                .classSessionId(UUID.randomUUID())
                .course(course)
                .sessionDate(date)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .status(SessionStatus.SCHEDULED)
                .attendanceClosed(false)
                .build();
    }

    private static ClassSessionDTO.CalendarResponse response(ClassSession session) {
        return new ClassSessionDTO.CalendarResponse(
                session.getClassSessionId(),
                session.getCourse().getCourseId(),
                session.getCourse().getName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.isAttendanceClosed(),
                null
        );
    }

    private static SessionAttendanceRepository.ClassSessionEvaluationRow evaluationRow(
            UUID enrollmentId,
            String fullName,
            UUID attendanceId,
            AttendanceStatus attendanceStatus,
            EvaluationStatus evaluationStatus
    ) {
        SessionAttendanceRepository.ClassSessionEvaluationRow row =
                mock(SessionAttendanceRepository.ClassSessionEvaluationRow.class);
        when(row.getStudentEnrollmentId()).thenReturn(enrollmentId);
        when(row.getStudentPersonId()).thenReturn(UUID.randomUUID());
        when(row.getStudentFullName()).thenReturn(fullName);
        when(row.getStudentGender()).thenReturn(true);
        when(row.getStudentBirthDate()).thenReturn(LocalDate.of(2015, 1, 1));
        when(row.getStudentPersonCode()).thenReturn("STU-" + enrollmentId);
        when(row.getStudentCurrentBelt()).thenReturn(Belt.C10);
        when(row.getStudentStatus()).thenReturn(PersonStatus.ACTIVE);
        when(row.getStudentFaceImagePath()).thenReturn(null);
        when(row.getCoursePurchaseId()).thenReturn(UUID.randomUUID());
        when(row.getEnrollmentStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(row.getEnrollmentEndDate()).thenReturn(LocalDate.of(2026, 12, 31));
        when(row.getEnrollmentStatus()).thenReturn(StudentEnrollmentStatus.ACTIVE);
        when(row.getSessionAttendanceId()).thenReturn(attendanceId);
        when(row.getCheckInTime()).thenReturn(attendanceId == null ? null : java.time.LocalDateTime.of(2026, 9, 30, 18, 5));
        when(row.getAttendanceStatus()).thenReturn(attendanceStatus);
        when(row.getEvaluationStatus()).thenReturn(evaluationStatus);
        when(row.getAttendanceNote()).thenReturn(attendanceId == null ? null : "OK");
        return row;
    }
}
