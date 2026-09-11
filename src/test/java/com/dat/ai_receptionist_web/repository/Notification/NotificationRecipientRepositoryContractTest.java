package com.dat.ai_receptionist_web.repository.Notification;

import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRecipientRepositoryContractTest {

    @Test
    void mineQueriesUseCurrentUserAndActivePersonScope() throws Exception {
        Query findMine = NotificationRecipientRepository.class
                .getMethod("findMine", UUID.class, UUID.class, Boolean.class, NotificationType.class,
                        String.class, org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class);
        Query findMineById = NotificationRecipientRepository.class
                .getMethod("findMineById", UUID.class, UUID.class, UUID.class)
                .getAnnotation(Query.class);

        assertVisibleScope(findMine.value());
        assertVisibleScope(findMineById.value());
        assertThat(findMine.value()).contains("order by nr.createdAt desc, nr.notificationRecipientId desc");
    }

    @Test
    void mineListFiltersAreOptionalAndScopedInsideTheSameQuery() throws Exception {
        Query findMine = NotificationRecipientRepository.class
                .getMethod("findMine", UUID.class, UUID.class, Boolean.class, NotificationType.class,
                        String.class, org.springframework.data.domain.Pageable.class)
                .getAnnotation(Query.class);

        assertThat(findMine.value())
                .contains("(:read is null or nr.read = :read)")
                .contains("(:type is null or nr.notification.notificationType = :type)")
                .contains(":search is null")
                .contains("lower(nr.notification.title) like lower(concat('%', :search, '%'))")
                .contains("lower(nr.notification.body) like lower(concat('%', :search, '%'))");
    }

    @Test
    void unreadAndReadAllQueriesUseSameCurrentScope() throws Exception {
        Query countUnread = NotificationRecipientRepository.class
                .getMethod("countUnreadMine", UUID.class, UUID.class)
                .getAnnotation(Query.class);
        Query markAllRead = NotificationRecipientRepository.class
                .getMethod("markAllRead", UUID.class, UUID.class, LocalDateTime.class)
                .getAnnotation(Query.class);

        assertVisibleScope(countUnread.value());
        assertVisibleScope(markAllRead.value());
        assertThat(countUnread.value()).contains("nr.read = false");
        assertThat(markAllRead.value())
                .contains("set nr.read = true", "nr.readAt = :readAt", "nr.read = false");
    }

    @Test
    void migrationUsesPartialUniqueIndexesForAccountAndContextRecipients() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V8__add_notification_recipient_context_person.sql"));

        assertThat(migration)
                .contains("ADD COLUMN context_person_id UUID NULL REFERENCES core.person(person_id)")
                .contains("DROP CONSTRAINT uk_notification_recipient")
                .contains("CREATE UNIQUE INDEX uk_notification_recipient_account")
                .contains("ON notification.notification_recipient(notification_id, recipient_user_id)")
                .contains("WHERE context_person_id IS NULL")
                .contains("CREATE UNIQUE INDEX uk_notification_recipient_context")
                .contains("ON notification.notification_recipient(notification_id, recipient_user_id, context_person_id)")
                .contains("WHERE context_person_id IS NOT NULL")
                .doesNotContain("NULLS NOT DISTINCT");
    }

    private void assertVisibleScope(String query) {
        assertThat(query)
                .contains("nr.recipientUser.userId = :userId")
                .contains(":activePersonId is null and nr.contextPerson is null")
                .contains(":activePersonId is not null")
                .contains("nr.contextPerson is null or nr.contextPerson.personId = :activePersonId")
                .doesNotContain("UserPerson up", "findActiveUserIdsByPersonId");
    }
}
