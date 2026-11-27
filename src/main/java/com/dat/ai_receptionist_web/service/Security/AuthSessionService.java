package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Core.Tenant;
import com.dat.ai_receptionist_web.domain.Core.TenantMembership;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.AuthSessionDTO;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.enums.Core.TenantMembershipStatus;
import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Core.UserPersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.AuthSessionMapper;
import com.dat.ai_receptionist_web.repository.Core.TenantMembershipRepository;
import com.dat.ai_receptionist_web.repository.Core.TenantRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.AuthSessionRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Core.SupabaseStorageService;
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
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final AuthSessionMapper authSessionMapper;
    private final UserPersonMapper userPersonMapper;
    private final SupabaseStorageService supabaseStorageService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenValidity;

    @Transactional(readOnly = true)
    public PageResponse<AuthSessionDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(sessionRepository.findAllDetailed(pageable), authSessionMapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public AuthSessionDTO.Response get(UUID id) {
        return authSessionMapper.toResponse(find(id));
    }

    @Transactional
    public AuthSessionDTO.Response create(AuthSessionDTO.CreateRequest request) {
        AuthSession session = new AuthSession();
        session.setUser(userRepository.findById(request.userId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND)));
        session.setTenantId(TenantContext.getTenantIdOrNull());
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

    @Transactional
    public void delete(UUID id) {
        revoke(find(id));
    }

    @Transactional
    public AuthSession create(UUID userId, UUID tenantId, String rawRefreshToken, String deviceInfo,
                              String platform, String fcmToken, UUID activeUserPersonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
        UserPerson active = null;
        if (activeUserPersonId != null) {
            if (tenantId == null) {
                throw new ApiException(SecurityErrorCode.TENANT_SELECTION_REQUIRED);
            }
            active = userPersonRepository
                    .findByUserPersonIdAndTenantIdAndUser_UserIdAndActiveTrue(activeUserPersonId, tenantId, userId)
                    .orElseThrow(() -> new ApiException(SecurityErrorCode.PERSON_CONTEXT_NOT_OWNED));
        }
        return sessionRepository.save(AuthSession.builder()
                .user(user)
                .tenantId(tenantId)
                .refreshTokenHash(RefreshTokenUtil.sha256(rawRefreshToken))
                .deviceInfo(deviceInfo)
                .platform(platform)
                .fcmToken(fcmToken)
                .activeUserPerson(active)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity))
                .revoked(false)
                .build());
    }

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

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessionRepository.findByRefreshTokenHash(RefreshTokenUtil.sha256(rawToken)).ifPresent(this::revoke);
    }

    @Transactional
    public void revoke(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(this::revoke);
    }

    @Transactional
    public void revokeAll(UUID userId) {
        sessionRepository.findAllByUser_UserIdAndRevokedFalse(userId).forEach(this::revoke);
    }

    @Transactional
    public TenantSwitchResult selectTenant(UUID userId, UUID sessionId, UUID targetTenantId) {
        AuthSession session = sessionRepository.findById(sessionId)
                .filter(value -> value.getUser().getUserId().equals(userId)
                        && !value.isRevoked()
                        && value.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ApiException(SecurityErrorCode.SESSION_NOT_ACTIVE));

        Tenant tenant = tenantRepository.findById(targetTenantId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.TENANT_NOT_FOUND));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ApiException(SecurityErrorCode.TENANT_INACTIVE);
        }

        TenantMembership membership = tenantMembershipRepository.findByTenantIdAndUser_UserId(targetTenantId, userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.TENANT_ACCESS_DENIED));
        if (membership.getStatus() != TenantMembershipStatus.ACTIVE) {
            throw new ApiException(SecurityErrorCode.TENANT_MEMBERSHIP_REVOKED);
        }

        session.setTenantId(targetTenantId);

        List<LoginRes.UserContextRes> availableContexts = TenantContext.runWithTenant(
                targetTenantId,
                () -> contexts(userId, targetTenantId));
        LoginRes.UserContextRes activeContext = availableContexts.size() == 1 ? availableContexts.getFirst() : null;
        if (activeContext == null) {
            session.setActiveUserPerson(null);
        } else {
            UserPerson activeEntity = userPersonRepository
                    .findByUserPersonIdAndTenantIdAndUser_UserIdAndActiveTrue(
                            activeContext.userPersonId(), targetTenantId, userId)
                    .orElse(null);
            session.setActiveUserPerson(activeEntity);
        }

        List<LoginRes.TenantRes> availableTenants = tenants(userId);
        LoginRes.TenantRes activeTenant = toTenantRes(tenant);
        return new TenantSwitchResult(activeTenant, availableTenants, activeContext, availableContexts);
    }

    @Transactional
    public ContextSwitchResult switchContext(UUID userId, UUID sessionId, UUID userPersonId) {
        AuthSession session = sessionRepository.findById(sessionId)
                .filter(value -> value.getUser().getUserId().equals(userId) && !value.isRevoked())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.SESSION_NOT_ACTIVE));
        UUID tenantId = session.getTenantId();
        if (tenantId == null) {
            throw new ApiException(SecurityErrorCode.TENANT_SELECTION_REQUIRED);
        }
        UserPerson target = userPersonRepository
                .findByUserPersonIdAndTenantIdAndUser_UserIdAndActiveTrue(userPersonId, tenantId, userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.PERSON_CONTEXT_NOT_OWNED));
        session.setActiveUserPerson(target);
        List<LoginRes.UserContextRes> available = contexts(userId, tenantId);
        return new ContextSwitchResult(toLoginContext(target), available);
    }

    @Transactional(readOnly = true)
    public List<LoginRes.TenantRes> tenants(UUID userId) {
        return tenantMembershipRepository.findActiveMembershipsForUser(userId).stream()
                .map(TenantMembership::getTenant)
                .map(this::toTenantRes)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoginRes.UserContextRes> contexts(UUID userId, UUID tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        return userPersonRepository.findAllByTenantIdAndUser_UserIdAndActiveTrue(tenantId, userId).stream()
                .map(this::toLoginContext).toList();
    }

    private LoginRes.TenantRes toTenantRes(Tenant tenant) {
        return new LoginRes.TenantRes(
                tenant.getTenantId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getStatus()
        );
    }

    private LoginRes.UserContextRes toLoginContext(UserPerson userPerson) {
        String faceImagePath = userPerson.getPerson() == null
                ? null
                : supabaseStorageService.getPublicUrl(userPerson.getPerson().getFaceImagePath());
        return userPersonMapper.toLoginContext(userPerson, faceImagePath);
    }

    @Transactional(readOnly = true)
    public List<AuthSession> sessions(UUID userId) {
        return sessionRepository.findAllByUser_UserId(userId);
    }

    @Transactional
    public void updateFcm(UUID sessionId, String token, String platform) {
        AuthSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.SESSION_NOT_FOUND));
        session.setFcmToken(token);
        if (platform != null) session.setPlatform(platform.toUpperCase(Locale.ROOT));
    }

    @Transactional(readOnly = true)
    public Set<String> fcmTokensForUser(UUID userId) {
        Set<String> result = new HashSet<>();
        sessionRepository.findAllByUser_UserIdAndRevokedFalse(userId).stream()
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(AuthSession::getFcmToken).filter(Objects::nonNull).forEach(result::add);
        return result;
    }

    private void revoke(AuthSession session) {
        if (!session.isRevoked()) {
            session.setRevoked(true);
            session.setRevokedAt(LocalDateTime.now());
        }
    }

    private AuthSession find(UUID id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.AUTH_SESSION_NOT_FOUND));
    }

    public record ContextSwitchResult(LoginRes.UserContextRes activeContext,
                                      List<LoginRes.UserContextRes> availableContexts) {
    }

    public record TenantSwitchResult(LoginRes.TenantRes activeTenant,
                                     List<LoginRes.TenantRes> availableTenants,
                                     LoginRes.UserContextRes activeContext,
                                     List<LoginRes.UserContextRes> availableContexts) {
    }
}
