package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Training.SessionAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Training.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Training.session.ClassSessionLifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClassSessionLifecycleServiceTest {
    private ClassSessionRepository classSessionRepository;
    private StudentEnrollmentRepository enrollmentRepository;
    private SessionAttendanceRepository attendanceRepository;
    private TransactionTemplate transactionTemplate;
    private ClassSessionLifecycleService service;

    @BeforeEach
    void setUp() {
        classSessionRepository = mock(ClassSessionRepository.class);
        enrollmentRepository = mock(StudentEnrollmentRepository.class);
        attendanceRepository = mock(SessionAttendanceRepository.class);
        transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        service = new ClassSessionLifecycleService(
                classSessionRepository, enrollmentRepository, attendanceRepository, transactionTemplate);
    }

    @Test
    void activatesSessionsUpToGraceThreshold() {
        when(classSessionRepository.activateScheduledSessions(any(), any())).thenReturn(3);

        int activated = service.activateSessions();

        assertThat(activated).isEqualTo(3);
        verify(classSessionRepository).activateScheduledSessions(
                any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    void completesOnlySessionsWhoseAttendanceIsAlreadyClosed() {
        ClassSession candidate = ClassSession.builder().classSessionId(UUID.randomUUID())
                .sessionDate(LocalDate.now()).startTime(LocalTime.of(0, 0))
                .endTime(LocalTime.of(0, 30)).status(SessionStatus.ACTIVE)
                .attendanceClosed(true).build();
        when(classSessionRepository.findSessionsToComplete(any(), any()))
                .thenReturn(List.of(candidate));
        when(classSessionRepository.markSessionCompleted(candidate.getClassSessionId()))
                .thenReturn(1);

        int completed = service.completeSessions();

        assertThat(completed).isEqualTo(1);
        verify(classSessionRepository).markSessionCompleted(candidate.getClassSessionId());
    }

    @Test
    void closesAttendanceCreatingAbsentOnlyForMissingEnrollments() {
        LocalDate today = LocalDate.now();
        Course course = Course.builder().courseId(UUID.randomUUID()).build();
        ClassSession session = ClassSession.builder().classSessionId(UUID.randomUUID())
                .course(course).sessionDate(today).status(SessionStatus.ACTIVE)
                .attendanceClosed(false).startTime(LocalTime.of(0, 0))
                .endTime(LocalTime.of(0, 30)).build();
        StudentEnrollment enrolled = StudentEnrollment.builder()
                .studentEnrollmentId(UUID.randomUUID()).build();
        StudentEnrollment missing = StudentEnrollment.builder()
                .studentEnrollmentId(UUID.randomUUID()).build();
        SessionAttendance existing = SessionAttendance.builder()
                .classSession(session).studentEnrollment(enrolled)
                .attendanceStatus(AttendanceStatus.PRESENT).build();

        when(classSessionRepository.findSessionsToClose(today)).thenReturn(List.of(session));
        when(classSessionRepository.findById(session.getClassSessionId()))
                .thenReturn(Optional.of(session));
        when(enrollmentRepository.findActiveEnrollmentsForCourseOnDate(
                course.getCourseId(), today)).thenReturn(List.of(enrolled, missing));
        when(attendanceRepository.findByClassSession_ClassSessionId(
                session.getClassSessionId())).thenReturn(List.of(existing));

        int closed = service.closeDueSessions();

        assertThat(closed).isEqualTo(1);
        ArgumentCaptor<List<SessionAttendance>> captor = ArgumentCaptor.forClass(List.class);
        verify(attendanceRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getStudentEnrollment().getStudentEnrollmentId())
                .isEqualTo(missing.getStudentEnrollmentId());
        assertThat(captor.getValue().get(0).getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(session.isAttendanceClosed()).isTrue();
        verify(classSessionRepository).save(session);
    }
}
