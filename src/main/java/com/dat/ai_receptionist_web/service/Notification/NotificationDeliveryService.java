package com.dat.ai_receptionist_web.service.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.service.Security.AuthSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
    private final NotificationRecipientRepository recipientRepository;
    private final AuthSessionService authSessionService;
    private final FirebaseNotificationSender sender;
    private final PlatformTransactionManager transactionManager;
    /**
     * Tác dụng: Thực hiện logic deliver của lớp hiện tại.
     * Input: Nhận UUID notificationId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void deliver(UUID notificationId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<Delivery> deliveries = transaction.execute(status ->
                recipientRepository.findDeliveryRows(notificationId).stream()
                        .map(row -> new Delivery(row.getNotificationRecipientId(),
                                row.getNotification().getNotificationId(),
                                row.getContextPerson() == null ? null : row.getContextPerson().getPersonId(),
                                row.getRecipientUser().getUserId(), row.getNotification().getTitle(),
                                row.getNotification().getBody(), row.getNotification().getPayload())).toList());
        if (deliveries == null) return;
        for (Delivery delivery : deliveries) {
            boolean sent = sender.send(authSessionService.fcmTokensForUser(delivery.userId()),
                    delivery.title(), delivery.body(), delivery.payload(),
                    delivery.recipientId(), delivery.notificationId(), delivery.contextPersonId());
            transaction.executeWithoutResult(status -> {
                NotificationRecipient recipient = recipientRepository.findById(delivery.recipientId()).orElseThrow();
                recipient.setNotificationRecipientStatus(sent
                        ? NotificationRecipientStatus.SENT : NotificationRecipientStatus.FAILED);
                if (sent) recipient.setDeliveredAt(LocalDateTime.now());
            });
        }
    }
    private record Delivery(UUID recipientId, UUID notificationId, UUID contextPersonId,
                            UUID userId, String title, String body, String payload) {}
}


