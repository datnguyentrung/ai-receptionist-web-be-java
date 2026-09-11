package com.dat.ai_receptionist_web.service.Notification;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Notification.Notification;
import com.dat.ai_receptionist_web.domain.Notification.NotificationRecipient;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationRecipientMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.enums.Training.NotificationRecipientStatus;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.AudienceContext;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.EligibilityResult;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.TargetSource;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {
    static final String DENIED_DETAIL = "Notification recipient is denied for this context";
    static final String UNSUPPORTED_DETAIL = "Notification recipient eligibility is unsupported for this context";
    static final String MARK_UNREAD_UNSUPPORTED_DETAIL = "Mark unread is not supported for notification recipients";

    private final NotificationRecipientRepository repository;
    private final NotificationRecipientMapper mapper;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final UserPersonRepository userPersonRepository;
    private final NotificationRecipientEligibilityPolicy eligibilityPolicy;
    private final CurrentAccessContextResolver currentAccessContextResolver;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationRecipientDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationRecipientDTO.Response> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public NotificationRecipientDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationRecipientDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationRecipientDTO.Response create(NotificationRecipientDTO.CreateRequest request) {
        Notification notification = notificationRepository.findById(request.notificationId())
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        User recipientUser = userRepository.findById(request.recipientUserId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
        Person contextPerson = request.contextPersonId() == null ? null : personRepository.findById(request.contextPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
        UserPerson candidate = contextPerson == null ? null : userPersonRepository
                .findByUser_UserIdAndPerson_PersonIdAndActiveTrue(recipientUser.getUserId(), contextPerson.getPersonId())
                .orElse(null);
        return mapper.toResponse(repository.save(createRecipient(
                notification,
                recipientUser,
                contextPerson,
                TargetSource.DIRECT_USER,
                candidate,
                contextPerson == null ? EligibilityResult.ALLOW : eligibilityPolicy.decide(
                        new NotificationRecipientEligibilityPolicy.Request(
                                notification.getNotificationType(),
                                notification.getReferenceType(),
                                notification.getReferenceId(),
                                TargetSource.PERSON,
                                candidate == null ? null : candidate.getRelationshipType(),
                                candidate,
                                AudienceContext.PERSON)))));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationRecipientDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationRecipientDTO.Response update(UUID id, NotificationRecipientDTO.UpdateRequest request) {
        var entity = find(id);
        mapper.updateEntity(request, entity);
        applyReadTransition(entity, request.read());
        validateLifecycle(entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationRecipientDTO.MineResponse> listMine(
            Boolean read, NotificationType type, String search, Pageable pageable) {
        AccessContext context = currentAccessContextResolver.current();

        String normalizedSearch = search == null || search.isBlank()
                ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

        return PageResponse.of(repository.findMine(
                        context.userId(),
                        context.activePersonId(),
                        read,
                        type,
                        normalizedSearch,
                        pageable),
                mapper::toMineResponse);
    }

    @Transactional(readOnly = true)
    public NotificationRecipientDTO.MineResponse getMine(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        return mapper.toMineResponse(repository
                .findMineById(id, context.userId(), context.activePersonId())
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public NotificationRecipientDTO.UnreadCountResponse unreadCountMine() {
        AccessContext context = currentAccessContextResolver.current();
        return new NotificationRecipientDTO.UnreadCountResponse(
                repository.countUnreadMine(context.userId(), context.activePersonId()));
    }

    @Transactional
    public NotificationRecipientDTO.UnreadCountResponse markRead(UUID id) {
        AccessContext context = currentAccessContextResolver.current();
        repository.markRead(id, context.userId(), context.activePersonId(), LocalDateTime.now());
        repository.findMineById(id, context.userId(), context.activePersonId())
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND));
        return new NotificationRecipientDTO.UnreadCountResponse(
                repository.countUnreadMine(context.userId(), context.activePersonId()));
    }

    @Transactional
    public NotificationRecipientDTO.UnreadCountResponse markAllRead() {
        AccessContext context = currentAccessContextResolver.current();
        repository.markAllRead(context.userId(), context.activePersonId(), LocalDateTime.now());
        return new NotificationRecipientDTO.UnreadCountResponse(
                repository.countUnreadMine(context.userId(), context.activePersonId()));
    }

    NotificationRecipient createRecipient(Notification notification, User recipientUser, Person contextPerson,
                                          TargetSource source, UserPerson candidateUserPerson,
                                          EligibilityResult eligibilityResult) {
        if (contextPerson != null && eligibilityResult != EligibilityResult.ALLOW) {
            throw notEligible(eligibilityResult);
        }
        return NotificationRecipient.builder()
                .notification(notification)
                .recipientUser(recipientUser)
                .contextPerson(contextPerson)
                .read(false)
                .readAt(null)
                .deliveredAt(null)
                .notificationRecipientStatus(NotificationRecipientStatus.PENDING)
                .build();
    }

    void saveAll(Iterable<NotificationRecipient> recipients) {
        repository.saveAll(recipients);
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        var entity = find(id);
        entity.setNotificationRecipientStatus(NotificationRecipientStatus.ARCHIVED);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationRecipient theo kết quả xử lý.
     */
    private NotificationRecipient find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_FOUND));
    }

    private void applyReadTransition(NotificationRecipient entity, boolean requestedRead) {
        if (!requestedRead) {
            if (entity.isRead()) {
                throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY, MARK_UNREAD_UNSUPPORTED_DETAIL);
            }
            entity.setRead(false);
            entity.setReadAt(null);
        } else if (!entity.isRead()) {
            entity.setRead(true);
            entity.setReadAt(LocalDateTime.now());
        }
    }

    private void validateLifecycle(NotificationRecipient entity) {
        if (!entity.isRead() && entity.getReadAt() != null) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY);
        }
        if (entity.getNotificationRecipientStatus() == NotificationRecipientStatus.PENDING
                && entity.getDeliveredAt() != null) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY);
        }
        if (entity.getNotificationRecipientStatus() == NotificationRecipientStatus.SENT
                && entity.getDeliveredAt() == null) {
            throw new ApiException(GeneralErrorCode.INVALID_REQUEST_BODY);
        }
    }

    static ApiException notEligible(EligibilityResult result) {
        return new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_NOT_ELIGIBLE,
                result == EligibilityResult.DENY ? DENIED_DETAIL : UNSUPPORTED_DETAIL);
    }
}


