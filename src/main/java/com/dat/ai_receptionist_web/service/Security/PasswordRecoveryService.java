package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.PasswordRecoveryReq;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Notification.FirebaseNotificationSender;
import com.dat.ai_receptionist_web.util.PhoneNumberUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESET_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_DELAY = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;
    private final AuthSessionService authSessionService;
    private final FirebaseNotificationSender firebaseNotificationSender;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public PasswordRecoveryReq.Options options(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(PhoneNumberUtil.normalize(phoneNumber)).orElse(null);
        UUID requestId = UUID.randomUUID();
        if (user == null) {
            put(
                    contextKey(requestId),
                    new ResetContext(
                            null,
                            List.of()
                    ),
                    CODE_TTL
            );
            return new PasswordRecoveryReq.Options(
                    requestId,
                    false,
                    List.of()
            );
        }
        List<EmailRef> emails = userPersonRepository.findAllByUser_UserIdAndActiveTrue(user.getUserId()).stream()
                .filter(this::hasVerifiedCurrentEmail)
                .map(value -> new EmailRef(
                        value.getUserPersonId(),
                        value.getPerson().getEmail()
                ))
                .distinct().toList();
        put(
                contextKey(requestId),
                new ResetContext(
                        user.getUserId(),
                        emails
                ),
                CODE_TTL
        );
        return new PasswordRecoveryReq.Options(
                requestId,
                !authSessionService.fcmTokensForUser(user.getUserId()).isEmpty(),
                emails.stream().map(value -> new PasswordRecoveryReq.EmailOption(
                        value.userPersonId(),
                        mask(value.email())
                )).toList()
        );
    }

    public PasswordRecoveryReq.Sent send(PasswordRecoveryReq.Send request) {
        ResetContext context = requiredContext(request.requestId());
        if (context.userId() == null) throw invalid();
        String cooldownKey = cooldownKey(request.requestId());
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey)))
            throw new ApiException(SecurityErrorCode.RATE_LIMIT_EXCEEDED);
        String code = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
        if (request.channel() == PasswordRecoveryReq.Channel.APP) {
            if (!firebaseNotificationSender.sendPasswordResetCode(
                    authSessionService.fcmTokensForUser(context.userId()),
                    code
            )) {
                throw new ApiException(SecurityErrorCode.PASSWORD_RESET_CHANNEL_UNAVAILABLE);
            }
        } else {
            EmailRef email = context.emails().stream().filter(value -> value.userPersonId().equals(request.userPersonId()))
                    .findFirst().orElseThrow(this::invalid);
            sendEmail(
                    email.email(),
                    code,
                    "Mã đặt lại mật khẩu"
            );
        }
        Instant now = Instant.now();
        put(
                codeKey(request.requestId()),
                new CodeState(
                        hash(code),
                        0
                ),
                CODE_TTL
        );
        redis.opsForValue().set(
                cooldownKey,
                "1",
                RESEND_DELAY
        );
        return new PasswordRecoveryReq.Sent(
                now.plus(RESEND_DELAY),
                now.plus(CODE_TTL)
        );
    }

    public PasswordRecoveryReq.Verified verify(PasswordRecoveryReq.Verify request) {
        ResetContext context = requiredContext(request.requestId());
        CodeState state = get(
                codeKey(request.requestId()),
                CodeState.class
        );
        if (state == null) throw new ApiException(SecurityErrorCode.PASSWORD_RESET_EXPIRED);
        if (state.attempts() >= MAX_ATTEMPTS || !MessageDigest.isEqual(
                state.hash().getBytes(StandardCharsets.UTF_8),
                hash(request.code()).getBytes(StandardCharsets.UTF_8)
        )) {
            put(
                    codeKey(request.requestId()),
                    new CodeState(
                            state.hash(),
                            state.attempts() + 1
                    ),
                    CODE_TTL
            );
            throw invalid();
        }
        String token = UUID.randomUUID().toString();
        put(
                resetKey(token),
                context.userId().toString(),
                RESET_TTL
        );
        redis.delete(List.of(
                contextKey(request.requestId()),
                codeKey(request.requestId()),
                cooldownKey(request.requestId())
        ));
        return new PasswordRecoveryReq.Verified(
                token,
                Instant.now().plus(RESET_TTL)
        );
    }

    @Transactional
    public void complete(
            PasswordRecoveryReq.Complete request,
            UserService userService
    ) {
        String rawUserId = redis.opsForValue().get(resetKey(request.resetToken()));
        if (rawUserId == null) throw new ApiException(SecurityErrorCode.PASSWORD_RESET_EXPIRED);
        userService.resetPassword(
                UUID.fromString(rawUserId),
                request.newPassword(),
                request.confirmPassword()
        );
        authSessionService.revokeAll(UUID.fromString(rawUserId));
        redis.delete(resetKey(request.resetToken()));
    }

    @Transactional(readOnly = true)
    public List<PasswordRecoveryReq.EmailVerification> recoveryEmails(UUID userId) {
        return userPersonRepository.findAllByUser_UserIdAndActiveTrue(userId).stream()
                .filter(value -> value.getPerson().getEmail() != null && !value.getPerson().getEmail().isBlank())
                .map(value -> new PasswordRecoveryReq.EmailVerification(
                        value.getUserPersonId(),
                        mask(value.getPerson().getEmail()),
                        hasVerifiedCurrentEmail(value)
                )).toList();
    }

    public PasswordRecoveryReq.Sent sendEmailVerification(
            UUID userId,
            UUID userPersonId
    ) {
        UserPerson person = ownedEmail(
                userId,
                userPersonId
        );
        String cooldownKey = verificationCooldownKey(
                userId,
                userPersonId
        );
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            throw new ApiException(SecurityErrorCode.RATE_LIMIT_EXCEEDED);
        }
        String code = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );
        sendEmail(
                person.getPerson().getEmail(),
                code,
                "Xác minh email khôi phục"
        );
        put(
                verificationKey(
                        userId,
                        userPersonId
                ),
                new CodeState(
                        hash(code),
                        0
                ),
                CODE_TTL
        );
        redis.opsForValue().set(
                cooldownKey,
                "1",
                RESEND_DELAY
        );
        Instant now = Instant.now();
        return new PasswordRecoveryReq.Sent(
                now.plus(RESEND_DELAY),
                now.plus(CODE_TTL)
        );
    }

    @Transactional
    public void verifyEmail(
            UUID userId,
            UUID userPersonId,
            String code
    ) {
        CodeState state = get(
                verificationKey(
                        userId,
                        userPersonId
                ),
                CodeState.class
        );
        if (state == null || state.attempts() >= MAX_ATTEMPTS || !MessageDigest.isEqual(
                state.hash().getBytes(StandardCharsets.UTF_8),
                hash(code).getBytes(StandardCharsets.UTF_8)
        )) throw invalid();
        UserPerson person = ownedEmail(
                userId,
                userPersonId
        );
        person.getPerson().setRecoveryEmailVerifiedValue(person.getPerson().getEmail());
        person.getPerson().setRecoveryEmailVerifiedAt(java.time.LocalDateTime.now());
        redis.delete(verificationKey(
                userId,
                userPersonId
        ));
        redis.delete(verificationCooldownKey(
                userId,
                userPersonId
        ));
    }

    private UserPerson ownedEmail(
            UUID userId,
            UUID userPersonId
    ) {
        return userPersonRepository.findByUserPersonIdAndUser_UserIdAndActiveTrue(
                        userPersonId,
                        userId
                )
                .filter(value -> value.getPerson().getEmail() != null && !value.getPerson().getEmail().isBlank()).orElseThrow(this::invalid);
    }

    private ResetContext requiredContext(UUID requestId) {
        ResetContext context = get(
                contextKey(requestId),
                ResetContext.class
        );
        if (context == null) throw new ApiException(SecurityErrorCode.PASSWORD_RESET_EXPIRED);
        return context;
    }

    private boolean hasVerifiedCurrentEmail(UserPerson value) {
        return value.getPerson().getEmail() != null && value.getPerson().getEmail().equalsIgnoreCase(value.getPerson().getRecoveryEmailVerifiedValue());
    }

    private void sendEmail(
            String to,
            String code,
            String subject
    ) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText("Mã OTP của bạn là: " + code + ". Mã có hiệu lực trong 5 phút.");
        mailSender.send(message);
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.charAt(0) + "***" + email.substring(at);
    }

    private ApiException invalid() {
        return new ApiException(SecurityErrorCode.PASSWORD_RESET_INVALID);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String contextKey(UUID id) {
        return "password-reset:context:" + id;
    }

    private String codeKey(UUID id) {
        return "password-reset:code:" + id;
    }

    private String cooldownKey(UUID id) {
        return "password-reset:cooldown:" + id;
    }

    private String resetKey(String token) {
        return "password-reset:token:" + token;
    }

    private String verificationKey(
            UUID userId,
            UUID personId
    ) {
        return "recovery-email:verify:" + userId + ':' + personId;
    }

    private String verificationCooldownKey(
            UUID userId,
            UUID personId
    ) {
        return "recovery-email:cooldown:" + userId + ':' + personId;
    }

    private void put(
            String key,
            Object value,
            Duration ttl
    ) {
        try {
            redis.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    ttl
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> T get(
            String key,
            Class<T> type
    ) {
        try {
            String raw = redis.opsForValue().get(key);
            return raw == null ? null : objectMapper.readValue(
                    raw,
                    type
            );
        } catch (Exception exception) {
            throw new ApiException(SecurityErrorCode.PASSWORD_RESET_EXPIRED);
        }
    }

    private record EmailRef(
            UUID userPersonId,
            String email
    ) {
    }

    private record ResetContext(
            UUID userId,
            List<EmailRef> emails
    ) {
    }

    private record CodeState(
            String hash,
            int attempts
    ) {
    }
}
