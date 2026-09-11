package com.dat.ai_receptionist_web.service.Notification;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Notification.*;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.NotificationErrorCode;
import com.dat.ai_receptionist_web.mapper.Notification.NotificationMapper;
import com.dat.ai_receptionist_web.repository.Notification.*;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.AudienceContext;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.EligibilityResult;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.ResolvedRecipientTarget;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientEligibilityPolicy.TargetSource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;
    private final NotificationRecipientService recipientService;
    private final NotificationRecipientEligibilityPolicy eligibilityPolicy;
    private final NotificationDeliveryService deliveryService;
    private final NotificationMapper notificationMapper;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationDTO.Response> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO.Response> list(Pageable pageable) {
        return PageResponse.of(notificationRepository.findAll(pageable), notificationMapper::toResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public NotificationDTO.Response get(UUID id) {
        return notificationMapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationDTO.Response create(NotificationDTO.CreateRequest request) {
        List<ResolvedTarget> targets = resolveTargets(request);
        if (targets.isEmpty()) {
            throw new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENT_REQUIRED);
        }
        Map<UUID, User> users = indexUsers(targets.stream()
                .map(target -> target.target().recipientUserId())
                .collect(java.util.stream.Collectors.toSet()));
        if (users.size() != targets.stream().map(target -> target.target().recipientUserId()).collect(java.util.stream.Collectors.toSet()).size())
            throw new ApiException(NotificationErrorCode.NOTIFICATION_RECIPIENTS_NOT_FOUND);
        Notification notification = notificationRepository.save(Notification.builder()
                .title(request.title()).body(request.body()).notificationType(request.type())
                .referenceType(request.referenceType()).referenceId(request.referenceId())
                .payload(request.payload()).build());
        List<NotificationRecipient> recipients = targets.stream()
                .map(resolved -> recipientService.createRecipient(
                        notification,
                        users.get(resolved.target().recipientUserId()),
                        resolved.candidateUserPerson() == null ? null : resolved.candidateUserPerson().getPerson(),
                        resolved.target().source(),
                        resolved.candidateUserPerson(),
                        EligibilityResult.ALLOW))
                .toList();
        recipientService.saveAll(recipients);
        afterCommitExecutor.afterCommit(() ->
                deliveryService.deliver(notification.getNotificationId()));
        return new NotificationDTO.Response(notification.getNotificationId(), recipients.size());
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public NotificationDTO.Response update(UUID id, NotificationDTO.UpdateRequest request) {
        Notification notification = find(id);
        notificationMapper.updateEntity(request, notification);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        notificationRepository.delete(find(id));
    }

    /**
     * Tác dụng: Thực hiện logic resolveRecipients của lớp hiện tại.
     * Input: Nhận NotificationDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về Set<UUID> theo kết quả xử lý.
     */
    private List<ResolvedTarget> resolveTargets(NotificationDTO.CreateRequest request) {
        Map<TargetKey, ResolvedTarget> targets = new LinkedHashMap<>();
        safe(request.recipientUserIds()).forEach(userId -> addTarget(targets,
                new ResolvedTarget(new ResolvedRecipientTarget(userId, null, TargetSource.DIRECT_USER, AudienceContext.ACCOUNT), null)));
        safe(request.recipientRoleCodes()).stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .forEach(roleCode -> userRepository.findUserIdsByRoleCode(roleCode).forEach(userId -> addTarget(targets,
                        new ResolvedTarget(new ResolvedRecipientTarget(userId, null, TargetSource.ROLE, AudienceContext.ACCOUNT), null))));
        resolvePersonTargets(request, targets);
        return List.copyOf(targets.values());
    }

    private void resolvePersonTargets(NotificationDTO.CreateRequest request,
                                      Map<TargetKey, ResolvedTarget> targets) {
        Set<UUID> personIds = safe(request.recipientPersonIds());
        if (personIds.isEmpty()) {
            return;
        }
        List<UserPerson> candidates = userPersonRepository.findActiveByPersonIds(personIds);
        for (UserPerson candidate : candidates) {
            EligibilityResult result = eligibilityPolicy.decide(new NotificationRecipientEligibilityPolicy.Request(
                    request.type(),
                    request.referenceType(),
                    request.referenceId(),
                    TargetSource.PERSON,
                    candidate.getRelationshipType(),
                    candidate,
                    AudienceContext.PERSON));
            if (result != EligibilityResult.ALLOW) {
                throw NotificationRecipientService.notEligible(result);
            }
            addTarget(targets, new ResolvedTarget(
                    new ResolvedRecipientTarget(
                            candidate.getUser().getUserId(),
                            candidate.getPerson().getPersonId(),
                            TargetSource.PERSON,
                            AudienceContext.PERSON),
                    candidate));
        }
        if (candidates.isEmpty()) {
            throw NotificationRecipientService.notEligible(EligibilityResult.UNSUPPORTED);
        }
    }

    private void addTarget(Map<TargetKey, ResolvedTarget> targets, ResolvedTarget resolved) {
        ResolvedRecipientTarget target = resolved.target();
        targets.putIfAbsent(new TargetKey(target.recipientUserId(), target.contextPersonId()), resolved);
    }

    private Map<UUID, User> indexUsers(Set<UUID> userIds) {
        Map<UUID, User> users = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> users.put(user.getUserId(), user));
        return users;
    }

    /**
     * Tác dụng: Thực hiện logic safe của lớp hiện tại.
     * Input: Nhận Set<T> values từ caller hoặc request.
     * Output: Trả về Set<T> theo kết quả xử lý.
     */
    private <T> Set<T> safe(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về Notification theo kết quả xử lý.
     */
    private Notification find(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private record TargetKey(UUID recipientUserId, UUID contextPersonId) {
    }

    private record ResolvedTarget(ResolvedRecipientTarget target, UserPerson candidateUserPerson) {
    }
}


