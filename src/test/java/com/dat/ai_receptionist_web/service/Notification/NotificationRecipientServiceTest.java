package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Notification.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationRecipientMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.EligibilityResult.DENY;
import static com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.EligibilityResult.UNSUPPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationRecipientServiceTest {

    private NotificationRecipientRepository repository;
    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private PersonRepository personRepository;
    private UserPersonRepository userPersonRepository;
    private NotificationRecipientEligibilityPolicy eligibilityPolicy;
    private CurrentAccessContextResolver currentAccessContextResolver;
    private NotificationRecipientMapper mapper;
    private NotificationRecipientService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRecipientRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        personRepository = mock(PersonRepository.class);
        userPersonRepository = mock(UserPersonRepository.class);
        eligibilityPolicy = mock(NotificationRecipientEligibilityPolicy.class);
        currentAccessContextResolver = mock(CurrentAccessContextResolver.class);
        mapper = mock(NotificationRecipientMapper.class, CALLS_REAL_METHODS);
        service = new NotificationRecipientService(
                repository,
                mapper,
                notificationRepository,
                userRepository,
                personRepository,
                userPersonRepository,
                eligibilityPolicy,
                currentAccessContextResolver);
    }

    @Test
    void managementCreateAccountLevelUsesServerOwnedLifecycleDefaults() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Notification notification = notification(notificationId);
        User user = User.builder().userId(userId).build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(NotificationRecipient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new NotificationRecipientDTO.CreateRequest(notificationId, userId, null));

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationRecipient.class);
        verify(repository).save(captor.capture());
        NotificationRecipient saved = captor.getValue();
        assertThat(saved.getNotification()).isSameAs(notification);
        assertThat(saved.getRecipientUser()).isSameAs(user);
        assertThat(saved.getContextPerson()).isNull();
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getReadAt()).isNull();
        assertThat(saved.getDeliveredAt()).isNull();
        assertThat(saved.getNotificationRecipientStatus()).isEqualTo(NotificationRecipientStatus.PENDING);
    }

    @Test
    void managementCreatePersonContextRejectsUnsupportedPolicy() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Notification notification = notification(notificationId);
        User user = User.builder().userId(userId).build();
        Person person = Person.builder().personId(personId).build();
        UserPerson userPerson = UserPerson.builder()
                .user(user)
                .person(person)
                .relationshipType(RelationshipType.GUARDIAN)
                .active(true)
                .build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(userPersonRepository.findByUser_UserIdAndPerson_PersonIdAndActiveTrue(userId, personId))
                .thenReturn(Optional.of(userPerson));
        when(eligibilityPolicy.decide(any())).thenReturn(UNSUPPORTED);

        assertThatThrownBy(() -> service.create(new NotificationRecipientDTO.CreateRequest(notificationId, userId, personId)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_ELIGIBLE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.responseDetail())
                                .isEqualTo(NotificationRecipientService.UNSUPPORTED_DETAIL));

        verify(repository, never()).save(any());
    }

    @Test
    void centralizedCreationRejectsDeniedPersonContext() {
        assertThatThrownBy(() -> service.createRecipient(
                notification(UUID.randomUUID()),
                User.builder().userId(UUID.randomUUID()).build(),
                Person.builder().personId(UUID.randomUUID()).build(),
                NotificationRecipientEligibilityPolicy.TargetSource.PERSON,
                null,
                DENY))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_ELIGIBLE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.responseDetail())
                                .isEqualTo(NotificationRecipientService.DENIED_DETAIL));
    }

    @Test
    void markReadRereadsCurrentScopeAndReturnsCount() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, RelationshipType.OWNER, Set.of(), Set.of()));
        when(repository.markRead(eq(recipientId), eq(userId), eq(activePersonId), any(LocalDateTime.class)))
                .thenReturn(1);
        when(repository.findMineById(recipientId, userId, activePersonId))
                .thenReturn(Optional.of(NotificationRecipient.builder().build()));
        when(repository.countUnreadMine(userId, activePersonId)).thenReturn(2L);

        NotificationRecipientDTO.UnreadCountResponse response = service.markRead(recipientId);

        assertThat(response.unreadCount()).isEqualTo(2);
    }

    @Test
    void listMineForwardsFiltersAndNormalizesSearch() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 30);
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, RelationshipType.OWNER, Set.of(), Set.of()));
        when(repository.findMine(userId, activePersonId, true, NotificationType.TUITION, "%abc%", pageable))
                .thenReturn(Page.<NotificationRecipient>empty(pageable));
        when(repository.findMine(userId, activePersonId, null, null, null, pageable))
                .thenReturn(Page.<NotificationRecipient>empty(pageable));

        service.listMine(true, NotificationType.TUITION, "  abc  ", pageable);
        service.listMine(null, null, "   ", pageable);

        verify(repository).findMine(userId, activePersonId, true, NotificationType.TUITION, "%abc%", pageable);
        verify(repository).findMine(userId, activePersonId, null, null, null, pageable);
    }

    @Test
    void getMineUsesCurrentScopeAndMapsRecipient() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        NotificationRecipient recipient = NotificationRecipient.builder()
                .notificationRecipientId(recipientId)
                .build();
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, RelationshipType.OWNER, Set.of(), Set.of()));
        when(repository.findMineById(recipientId, userId, activePersonId)).thenReturn(Optional.of(recipient));
        when(mapper.toMineResponse(recipient)).thenReturn(new NotificationRecipientDTO.MineResponse(
                recipientId,
                UUID.randomUUID(),
                activePersonId,
                "title",
                "body",
                NotificationType.SYSTEM,
                null,
                null,
                null,
                false,
                null,
                null,
                NotificationRecipientStatus.SENT,
                LocalDateTime.now()));

        NotificationRecipientDTO.MineResponse response = service.getMine(recipientId);

        assertThat(response.notificationRecipientId()).isEqualTo(recipientId);
        assertThat(response.contextPersonId()).isEqualTo(activePersonId);
    }

    @Test
    void getMineCrossContextReturnsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, RelationshipType.OWNER, Set.of(), Set.of()));
        when(repository.findMineById(recipientId, userId, activePersonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMine(recipientId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND));
    }

    @Test
    void markReadCrossContextReturnsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID activePersonId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        when(currentAccessContextResolver.current()).thenReturn(new AccessContext(
                userId, UUID.randomUUID(), activePersonId, RelationshipType.OWNER, Set.of(), Set.of()));
        when(repository.markRead(eq(recipientId), eq(userId), eq(activePersonId), any(LocalDateTime.class)))
                .thenReturn(0);
        when(repository.findMineById(recipientId, userId, activePersonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(recipientId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND));
    }

    @Test
    void updateReadFalseRejectsMarkUnread() {
        UUID recipientId = UUID.randomUUID();
        NotificationRecipient recipient = NotificationRecipient.builder()
                .notificationRecipientId(recipientId)
                .read(true)
                .readAt(LocalDateTime.now().minusDays(1))
                .notificationRecipientStatus(NotificationRecipientStatus.PENDING)
                .build();
        when(repository.findById(recipientId)).thenReturn(Optional.of(recipient));

        assertThatThrownBy(() -> service.update(recipientId, new NotificationRecipientDTO.UpdateRequest(
                false,
                null,
                NotificationRecipientStatus.PENDING)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.responseDetail())
                                .isEqualTo(NotificationRecipientService.MARK_UNREAD_UNSUPPORTED_DETAIL));

        assertThat(recipient.isRead()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void updateReadTrueSetsServerReadAt() {
        UUID recipientId = UUID.randomUUID();
        NotificationRecipient recipient = NotificationRecipient.builder()
                .notificationRecipientId(recipientId)
                .read(false)
                .notificationRecipientStatus(NotificationRecipientStatus.PENDING)
                .build();
        when(repository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(repository.save(recipient)).thenReturn(recipient);

        service.update(recipientId, new NotificationRecipientDTO.UpdateRequest(
                true,
                null,
                NotificationRecipientStatus.PENDING));

        assertThat(recipient.isRead()).isTrue();
        assertThat(recipient.getReadAt()).isNotNull();
    }

    @Test
    void updateAlreadyReadTruePreservesOriginalReadAt() {
        UUID recipientId = UUID.randomUUID();
        LocalDateTime originalReadAt = LocalDateTime.now().minusDays(1);
        NotificationRecipient recipient = NotificationRecipient.builder()
                .notificationRecipientId(recipientId)
                .read(true)
                .readAt(originalReadAt)
                .notificationRecipientStatus(NotificationRecipientStatus.PENDING)
                .build();
        when(repository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(repository.save(recipient)).thenReturn(recipient);

        service.update(recipientId, new NotificationRecipientDTO.UpdateRequest(
                true,
                null,
                NotificationRecipientStatus.PENDING));

        assertThat(recipient.isRead()).isTrue();
        assertThat(recipient.getReadAt()).isEqualTo(originalReadAt);
    }

    private Notification notification(UUID notificationId) {
        return Notification.builder()
                .notificationId(notificationId)
                .title("title")
                .body("body")
                .notificationType(NotificationType.SYSTEM)
                .referenceType("SYSTEM")
                .referenceId("1")
                .payload("{}")
                .build();
    }
}
