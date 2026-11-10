package com.dat.ai_receptionist_web.repository.Notification;
import com.dat.ai_receptionist_web.domain.Notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
 void deleteByReferenceTypeAndReferenceIdIn(String referenceType, java.util.Collection<String> referenceIds);
}
