package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.AuthSessionDTO;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.UserPersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.AuthSessionMapper;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.*;
import com.dat.ai_receptionist_web.util.RefreshTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final AuthSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final UserPersonRepository userPersonRepository;
    private final AuthSessionMapper authSessionMapper;
    private final UserPersonMapper userPersonMapper;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidity;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<AuthSessionDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<AuthSessionDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(sessionRepository.findAllDetailed(pageable), authSessionMapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về AuthSessionDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public AuthSessionDTO.Response get(UUID id) {
        return authSessionMapper.toResponse(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận AuthSessionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về AuthSessionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public AuthSessionDTO.Response create(AuthSessionDTO.CreateRequest request) {
        AuthSession session = new AuthSession();
        session.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        session.setActiveUserPerson(request.activeUserPersonId() == null ? null
                : userPersonRepository.findById(request.activeUserPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.USER_PERSON_NOT_FOUND)));
        session.setRefreshTokenHash(request.refreshTokenHash());
        session.setDeviceInfo(request.deviceInfo());
        session.setPlatform(request.platform());
        session.setFcmToken(request.fcmToken());
        session.setExpiresAt(request.expiresAt());
        session.setRevoked(request.revoked());
        session.setRevokedAt(request.revokedAt());
        session.setVersion(request.version());
        return authSessionMapper.toResponse(sessionRepository.save(session));
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, AuthSessionDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về AuthSessionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public AuthSessionDTO.Response update(UUID id, AuthSessionDTO.UpdateRequest request) {
        AuthSession session = find(id);
        session.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        session.setActiveUserPerson(request.activeUserPersonId() == null ? null
                : userPersonRepository.findById(request.activeUserPersonId())
                .orElseThrow(() -> new ApiException(CoreErrorCode.USER_PERSON_NOT_FOUND)));
        authSessionMapper.updateEntity(request, session);
        return authSessionMapper.toResponse(sessionRepository.save(session));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID id) {
        revoke(find(id));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận UUID userId, String rawRefreshToken, String deviceInfo, String platform, String fcmToken, UUID activeUserPersonId từ caller hoặc request.
     * Output: Trả về AuthSession theo kết quả xử lý.
     */
    @Transactional
    public AuthSession create(UUID userId, String rawRefreshToken, String deviceInfo,
                              String platform, String fcmToken, UUID activeUserPersonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
        UserPerson active = activeUserPersonId == null ? null
                : userPersonRepository.findByUserPersonIdAndUser_UserIdAndActiveTrue(activeUserPersonId, userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.PERSON_CONTEXT_NOT_OWNED));
        return sessionRepository.save(AuthSession.builder()
                .user(user)
                .refreshTokenHash(RefreshTokenUtil.sha256(rawRefreshToken))
                .deviceInfo(deviceInfo)
                .platform(platform)
                .fcmToken(fcmToken)
                .activeUserPerson(active)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity))
                .revoked(false)
                .build());
    }

    /**
     * Tác dụng: Xoay vòng token hoặc trạng thái hiện tại sang giá trị mới an toàn hơn.
     * Input: Nhận String rawCurrentToken, String rawNewToken từ caller hoặc request.
     * Output: Trả về AuthSession theo kết quả xử lý.
     */
    @Transactional
    public AuthSession rotate(String rawCurrentToken, String rawNewToken) {
        AuthSession session = sessionRepository
                .findByRefreshTokenHashForUpdate(RefreshTokenUtil.sha256(rawCurrentToken))
                .orElseThrow(() -> new ApiException(SecurityErrorCode.INVALID_REFRESH_TOKEN));
        if (session.isRevoked() || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ApiException(SecurityErrorCode.REFRESH_SESSION_INACTIVE);
        }
        session.setRefreshTokenHash(RefreshTokenUtil.sha256(rawNewToken));
        session.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity));
        return session;
    }

    /**
     * Tác dụng: Thu hồi phiên hoặc quyền truy cập theo điều kiện đầu vào.
     * Input: Nhận String rawToken từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessionRepository.findByRefreshTokenHash(RefreshTokenUtil.sha256(rawToken)).ifPresent(this::revoke);
    }

    /**
     * Tác dụng: Thu hồi phiên hoặc quyền truy cập theo điều kiện đầu vào.
     * Input: Nhận UUID sessionId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void revoke(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(this::revoke);
    }

    /**
     * Tác dụng: Thu hồi phiên hoặc quyền truy cập theo điều kiện đầu vào.
     * Input: Nhận UUID userId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void revokeAll(UUID userId) {
        sessionRepository.findAllByUser_UserIdAndRevokedFalse(userId).forEach(this::revoke);
    }

    /**
     * Tác dụng: Chuyển ngữ cảnh hoạt động của người dùng sau khi kiểm tra quyền sở hữu.
     * Input: Nhận UUID userId, UUID sessionId, UUID userPersonId từ caller hoặc request.
     * Output: Trả về ContextSwitchResult theo kết quả xử lý.
     */
    @Transactional
    public ContextSwitchResult switchContext(UUID userId, UUID sessionId, UUID userPersonId) {
        AuthSession session = sessionRepository.findById(sessionId)
                .filter(value -> value.getUser().getUserId().equals(userId) && !value.isRevoked())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.SESSION_NOT_ACTIVE));
        UserPerson target = userPersonRepository
                .findByUserPersonIdAndUser_UserIdAndActiveTrue(userPersonId, userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.PERSON_CONTEXT_NOT_OWNED));
        session.setActiveUserPerson(target);
        List<LoginRes.UserContextRes> available = contexts(userId);
        return new ContextSwitchResult(userPersonMapper.toLoginContext(target), available);
    }

    /**
     * Tác dụng: Thực hiện logic contexts của lớp hiện tại.
     * Input: Nhận UUID userId từ caller hoặc request.
     * Output: Trả về List<LoginRes.UserContextRes> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public List<LoginRes.UserContextRes> contexts(UUID userId) {
        return userPersonRepository.findAllByUser_UserIdAndActiveTrue(userId).stream()
                .map(userPersonMapper::toLoginContext).toList();
    }

    /**
     * Tác dụng: Thực hiện logic sessions của lớp hiện tại.
     * Input: Nhận UUID userId từ caller hoặc request.
     * Output: Trả về List<AuthSession> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public List<AuthSession> sessions(UUID userId) {
        return sessionRepository.findAllByUser_UserId(userId);
    }

    /**
     * Tác dụng: Thực hiện logic updateFcm của lớp hiện tại.
     * Input: Nhận UUID sessionId, String token, String platform từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void updateFcm(UUID sessionId, String token, String platform) {
        AuthSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.SESSION_NOT_FOUND));
        session.setFcmToken(token);
        if (platform != null) session.setPlatform(platform.toUpperCase(Locale.ROOT));
    }

    /**
     * Tác dụng: Thực hiện logic fcmTokensForUser của lớp hiện tại.
     * Input: Nhận UUID userId từ caller hoặc request.
     * Output: Trả về Set<String> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public Set<String> fcmTokensForUser(UUID userId) {
        Set<String> result = new HashSet<>();
        sessionRepository.findAllByUser_UserIdAndRevokedFalse(userId).stream()
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(AuthSession::getFcmToken).filter(Objects::nonNull).forEach(result::add);
        return result;
    }

    /**
     * Tác dụng: Thu hồi phiên hoặc quyền truy cập theo điều kiện đầu vào.
     * Input: Nhận AuthSession session từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void revoke(AuthSession session) {
        if (!session.isRevoked()) {
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
        }
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về AuthSession theo kết quả xử lý.
     */
    private AuthSession find(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.AUTH_SESSION_NOT_FOUND));
    }

    public record ContextSwitchResult(LoginRes.UserContextRes activeContext,
                                      List<LoginRes.UserContextRes> availableContexts) {
    }
}


