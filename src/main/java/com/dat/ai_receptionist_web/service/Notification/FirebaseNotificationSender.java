package com.dat.ai_receptionist_web.service.Notification;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.*;
@Slf4j
@Service
public class FirebaseNotificationSender {
    private final FirebaseMessaging firebaseMessaging;
    /**
     * Tác dụng: Thực hiện logic FirebaseNotificationSender của lớp hiện tại.
     * Input: Nhận ObjectProvider<FirebaseMessaging> provider từ caller hoặc request.
     * Output: Khởi tạo instance của lớp với các phụ thuộc đầu vào.
     */
    public FirebaseNotificationSender(ObjectProvider<FirebaseMessaging> provider) {
        this.firebaseMessaging = provider.getIfAvailable();
    }
    /**
     * Tác dụng: Thực hiện logic send của lớp hiện tại.
     * Input: Nhận Set<String> tokens, String title, String body, String payload từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    public boolean send(Set<String> tokens, String title, String body, String payload,
                        UUID notificationRecipientId, UUID notificationId, UUID contextPersonId) {
        if (tokens.isEmpty() || firebaseMessaging == null) return false;
        try {
            MulticastMessage.Builder message = MulticastMessage.builder().addAllTokens(tokens)
                    .putData("title", title).putData("body", body)
                    .putData("notificationRecipientId", notificationRecipientId.toString())
                    .putData("notificationId", notificationId.toString());
            if (contextPersonId != null) message.putData("contextPersonId", contextPersonId.toString());
            if (payload != null) message.putData("payload", payload);
            return firebaseMessaging.sendEachForMulticast(message.build()).getSuccessCount() > 0;
        } catch (FirebaseMessagingException exception) {
            log.error("FCM delivery failed for {} tokens", tokens.size(), exception);
            return false;
        }
    }

    public boolean sendPasswordResetCode(Set<String> tokens, String code) {
        if (tokens.isEmpty() || firebaseMessaging == null) return false;
        try {
            MulticastMessage message = MulticastMessage.builder().addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle("Mã đặt lại mật khẩu")
                            .setBody("Mã OTP của bạn: " + code + ". Có hiệu lực trong 5 phút.")
                            .build())
                    .putData("type", "PASSWORD_RESET_CODE")
                    .putData("code", code)
                    .build();
            return firebaseMessaging.sendEachForMulticast(message).getSuccessCount() > 0;
        } catch (FirebaseMessagingException exception) {
            log.error("Password reset FCM delivery failed for {} tokens", tokens.size(), exception);
            return false;
        }
    }
}


