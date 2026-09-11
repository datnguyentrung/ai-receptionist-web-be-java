package com.dat.ai_receptionist_web.repository.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {
    @EntityGraph(attributePaths = {"notification", "recipientUser", "contextPerson"})
    @Query("select nr from NotificationRecipient nr where nr.notification.notificationId = :id")
    List<NotificationRecipient> findDeliveryRows(@Param("id") UUID notificationId);

    @EntityGraph(attributePaths = {"notification", "recipientUser", "contextPerson"})
    @Query("""
        select nr
        from NotificationRecipient nr
        where nr.recipientUser.userId = :userId
          and (
            (:activePersonId is null and nr.contextPerson is null)
            or
            (:activePersonId is not null and (
                nr.contextPerson is null
                or nr.contextPerson.personId = :activePersonId
            ))
          )
          and (:read is null or nr.read = :read)
          and (:type is null or nr.notification.notificationType = :type)
          and (
            :searchPattern is null
            or lower(nr.notification.title) like :searchPattern
            or lower(nr.notification.body) like :searchPattern
          )
        order by nr.createdAt desc, nr.notificationRecipientId desc
        """)
    Page<NotificationRecipient> findMine(
            @Param("userId") UUID userId,
            @Param("activePersonId") UUID activePersonId,
            @Param("read") Boolean read,
            @Param("type") NotificationType type,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"notification", "recipientUser", "contextPerson"})
    @Query("""
            select nr
            from NotificationRecipient nr
            where nr.notificationRecipientId = :id
              and nr.recipientUser.userId = :userId
              and (
                (:activePersonId is null and nr.contextPerson is null)
                or
                (:activePersonId is not null and (nr.contextPerson is null or nr.contextPerson.personId = :activePersonId))
              )
            """)
    Optional<NotificationRecipient> findMineById(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("activePersonId") UUID activePersonId);

    @Query("""
            select count(nr)
            from NotificationRecipient nr
            where nr.recipientUser.userId = :userId
              and nr.read = false
              and (
                (:activePersonId is null and nr.contextPerson is null)
                or
                (:activePersonId is not null and (nr.contextPerson is null or nr.contextPerson.personId = :activePersonId))
              )
            """)
    long countUnreadMine(@Param("userId") UUID userId, @Param("activePersonId") UUID activePersonId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationRecipient nr
            set nr.read = true,
                nr.readAt = :readAt
            where nr.notificationRecipientId = :id
              and nr.recipientUser.userId = :userId
              and nr.read = false
              and (
                (:activePersonId is null and nr.contextPerson is null)
                or
                (:activePersonId is not null and (nr.contextPerson is null or nr.contextPerson.personId = :activePersonId))
              )
            """)
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId,
                 @Param("activePersonId") UUID activePersonId, @Param("readAt") java.time.LocalDateTime readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationRecipient nr
            set nr.read = true,
                nr.readAt = :readAt
            where nr.recipientUser.userId = :userId
              and nr.read = false
              and (
                (:activePersonId is null and nr.contextPerson is null)
                or
                (:activePersonId is not null and (nr.contextPerson is null or nr.contextPerson.personId = :activePersonId))
              )
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("activePersonId") UUID activePersonId,
                    @Param("readAt") java.time.LocalDateTime readAt);
}
