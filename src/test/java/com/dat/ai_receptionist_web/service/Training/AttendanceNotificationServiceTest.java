package com.dat.ai_receptionist_web.service.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.domain.Training.SessionAttendance;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import com.dat.ai_receptionist_web.service.Notification.TransactionAfterCommitExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttendanceNotificationServiceTest {
    private final UserPersonRepository userPersonRepository = mock(UserPersonRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final TransactionAfterCommitExecutor afterCommitExecutor = mock(TransactionAfterCommitExecutor.class);
    private final AttendanceNotificationService service = new AttendanceNotificationService(
            userPersonRepository,
            notificationService,
            afterCommitExecutor,
            new ObjectMapper()
    );

    @Test
    void attendanceNotificationTargetsAllActiveLinkedAccountsAfterCommit() {
        UUID personId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        SessionAttendance attendance = attendance(personId);
        when(userPersonRepository.findActiveUserIdsByPersonId(personId))
                .thenReturn(List.of(firstUserId, secondUserId));

        service.notifyAttendanceRecorded(attendance);

        ArgumentCaptor<Runnable> action = ArgumentCaptor.forClass(Runnable.class);
        verify(afterCommitExecutor).afterCommit(action.capture());
        verify(notificationService, never()).create(any());

        action.getValue().run();

        ArgumentCaptor<NotificationDTO.CreateRequest> request = ArgumentCaptor.forClass(NotificationDTO.CreateRequest.class);
        verify(notificationService).create(request.capture());
        assertThat(request.getValue().notificationType()).isEqualTo(NotificationType.ATTENDANCE);
        assertThat(request.getValue().recipientUserIds()).containsExactlyInAnyOrder(firstUserId, secondUserId);
        assertThat(request.getValue().referenceType()).isEqualTo("SESSION_ATTENDANCE");
        assertThat(request.getValue().payload()).contains("ATTENDANCE_RECORDED");
    }

    @Test
    void skipsNotificationWhenThePersonHasNoActiveLinkedAccount() {
        UUID personId = UUID.randomUUID();
        when(userPersonRepository.findActiveUserIdsByPersonId(personId)).thenReturn(List.of());

        service.notifyEvaluationUpdated(attendance(personId));

        verify(afterCommitExecutor, never()).afterCommit(any());
        verify(notificationService, never()).create(any());
    }

    private SessionAttendance attendance(UUID personId) {
        Person person = Person.builder().personId(personId).fullName("Nguyễn Văn A").build();
        StudentEnrollment enrollment = StudentEnrollment.builder().studentPerson(person).build();
        ClassSession session = ClassSession.builder().classSessionId(UUID.randomUUID()).build();
        return SessionAttendance.builder()
                .sessionAttendanceId(UUID.randomUUID())
                .classSession(session)
                .studentEnrollment(enrollment)
                .attendanceStatus(AttendanceStatus.PRESENT)
                .build();
    }
}
