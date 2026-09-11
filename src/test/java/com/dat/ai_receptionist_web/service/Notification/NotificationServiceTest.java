package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Notification.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationMapper;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.EligibilityResult.ALLOW;
import static com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.EligibilityResult.UNSUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Test
    void createDirectUserCreatesAccountLevelRecipient() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserPersonRepository userPersonRepository = mock(UserPersonRepository.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);
        TransactionAfterCommitExecutor afterCommitExecutor = mock(TransactionAfterCommitExecutor.class);

        NotificationService service = new NotificationService(
                notificationRepository,
                userRepository,
                userPersonRepository,
                recipientService,
                mock(NotificationRecipientEligibilityPolicy.class),
                mock(NotificationDeliveryService.class),
                mock(NotificationMapper.class),
                afterCommitExecutor);

        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        when(userRepository.findAllById(Set.of(userId))).thenReturn(List.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recipientService.createRecipient(any(), eq(user), isNull(), any(), isNull(), eq(ALLOW)))
                .thenAnswer(invocation -> NotificationRecipient.builder()
                        .notification(invocation.getArgument(0))
                        .recipientUser(user)
                        .build());

        NotificationDTO.Response response = service.create(new NotificationDTO.CreateRequest(
                "title", "body", NotificationType.CLASS_SCHEDULE, "COURSE_SCHEDULE_CHANGE",
                UUID.randomUUID().toString(), "{}", Set.of(userId), null, null));

        assertThat(response.recipientCount()).isEqualTo(1);
        ArgumentCaptor<Iterable<NotificationRecipient>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(recipientService).saveAll(captor.capture());
        NotificationRecipient recipient = captor.getValue().iterator().next();
        assertThat(recipient.getRecipientUser().getUserId()).isEqualTo(userId);
        assertThat(recipient.getContextPerson()).isNull();
        verify(afterCommitExecutor).afterCommit(any(Runnable.class));
    }

    @Test
    void createRoleTargetCreatesAccountLevelRecipients() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationRecipientService recipientService = mock(NotificationRecipientService.class);

        NotificationService service = new NotificationService(
                notificationRepository,
                userRepository,
                mock(UserPersonRepository.class),
                recipientService,
                mock(NotificationRecipientEligibilityPolicy.class),
                mock(NotificationDeliveryService.class),
                mock(NotificationMapper.class),
                mock(TransactionAfterCommitExecutor.class));

        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        when(userRepository.findUserIdsByRoleCode("SYSTEM_ADMIN")).thenReturn(List.of(userId));
        when(userRepository.findAllById(Set.of(userId))).thenReturn(List.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recipientService.createRecipient(any(), eq(user), isNull(), any(), isNull(), eq(ALLOW)))
                .thenReturn(NotificationRecipient.builder().recipientUser(user).build());

        NotificationDTO.Response response = service.create(new NotificationDTO.CreateRequest(
                "schedule changed", "body", NotificationType.CLASS_SCHEDULE, "COURSE_SCHEDULE_CHANGE",
                UUID.randomUUID().toString(), "{}", null, null, Set.of("system_admin")));

        assertThat(response.recipientCount()).isEqualTo(1);
        verify(recipientService).createRecipient(any(), eq(user), isNull(), any(), isNull(), eq(ALLOW));
    }

    @Test
    void unsupportedPersonTargetFailsBeforeSideEffects() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        UserPersonRepository userPersonRepository = mock(UserPersonRepository.class);
        NotificationRecipientEligibilityPolicy eligibilityPolicy = mock(NotificationRecipientEligibilityPolicy.class);

        NotificationService service = new NotificationService(
                notificationRepository,
                mock(UserRepository.class),
                userPersonRepository,
                mock(NotificationRecipientService.class),
                eligibilityPolicy,
                mock(NotificationDeliveryService.class),
                mock(NotificationMapper.class),
                mock(TransactionAfterCommitExecutor.class));

        UUID personId = UUID.randomUUID();
        UserPerson candidate = UserPerson.builder()
                .user(User.builder().userId(UUID.randomUUID()).build())
                .person(Person.builder().personId(personId).build())
                .relationshipType(RelationshipType.GUARDIAN)
                .active(true)
                .build();
        when(userPersonRepository.findActiveByPersonIds(Set.of(personId))).thenReturn(List.of(candidate));
        when(eligibilityPolicy.decide(any())).thenReturn(UNSUPPORTED);

        assertThatThrownBy(() -> service.create(new NotificationDTO.CreateRequest(
                "title", "body", NotificationType.ATTENDANCE, "STUDENT_CHECK_IN",
                UUID.randomUUID().toString(), "{}", null, Set.of(personId), null)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_ELIGIBLE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.responseDetail())
                                .isEqualTo(NotificationRecipientService.UNSUPPORTED_DETAIL));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void mixedUnsupportedPersonTargetFailsWithoutPartialDirectRecipient() {
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        UserPersonRepository userPersonRepository = mock(UserPersonRepository.class);

        NotificationService service = new NotificationService(
                notificationRepository,
                mock(UserRepository.class),
                userPersonRepository,
                mock(NotificationRecipientService.class),
                new NotificationRecipientEligibilityPolicy(),
                mock(NotificationDeliveryService.class),
                mock(NotificationMapper.class),
                mock(TransactionAfterCommitExecutor.class));

        when(userPersonRepository.findActiveByPersonIds(anySet())).thenReturn(List.of(UserPerson.builder()
                .user(User.builder().userId(UUID.randomUUID()).build())
                .person(Person.builder().personId(UUID.randomUUID()).build())
                .relationshipType(RelationshipType.OWNER)
                .active(true)
                .build()));

        assertThatThrownBy(() -> service.create(new NotificationDTO.CreateRequest(
                "title", "body", NotificationType.SYSTEM, "SYSTEM", "1", "{}",
                Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()), null)))
                .isInstanceOf(ApiException.class);

        verify(notificationRepository, never()).save(any());
    }
}
