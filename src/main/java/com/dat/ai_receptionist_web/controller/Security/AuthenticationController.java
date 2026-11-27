package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import com.dat.ai_receptionist_web.dto.Security.*;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Security.AuthenticationMapper;
import com.dat.ai_receptionist_web.service.Security.*;
import com.dat.ai_receptionist_web.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final AuthSessionService sessionService;
    private final SecurityUtil securityUtil;
    private final AuthenticationMapper authenticationMapper;
    private final PasswordRecoveryService passwordRecoveryService;

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;
    @Value("${auth.refresh-cookie.secure:true}")
    private boolean refreshCookieSecure;
    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq.UserBase request) {
        LoginBundle bundle = authenticate(request, "WEB");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(bundle.refreshToken()).toString())
                .body(bundle.response());
    }

    @PostMapping("/mobile/login")
    public ResponseEntity<LoginRes.MobileResponse> mobileLogin(
            @Valid @RequestBody LoginReq.MobileLoginRequest request) {
        LoginBundle bundle = authenticate(request, request.getPlatform().toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(authenticationMapper.toMobileResponse(bundle.response(), bundle.refreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginRes> refresh(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String rawToken) {
        try {
            LoginBundle bundle = refreshBundle(rawToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie(bundle.refreshToken()).toString())
                    .body(bundle.response());
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
        }
    }

    @PostMapping("/mobile/refresh")
    public ResponseEntity<LoginRes.MobileResponse> mobileRefresh(
            @Valid @RequestBody LoginReq.RefreshTokenRequest request) {
        try {
            LoginBundle bundle = refreshBundle(request.getRefreshToken());
            return ResponseEntity.ok(authenticationMapper.toMobileResponse(bundle.response(), bundle.refreshToken()));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/tenants")
    public List<LoginRes.TenantRes> tenants() {
        return sessionService.tenants(currentUserId());
    }

    @PostMapping("/tenants/select")
    public LoginRes selectTenant(@Valid @RequestBody LoginReq.SelectTenantReq request) {
        UUID userId = currentUserId();
        UUID sessionId = currentSessionId();
        AuthSessionService.TenantSwitchResult switched =
                sessionService.selectTenant(userId, sessionId, request.getTenantId());
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId, request.getTenantId());
        UUID activePersonId = switched.activeContext() == null ? null : switched.activeContext().userPersonId();
        String accessToken = securityUtil.createAccessToken(sessionId, snapshot, activePersonId);
        return authenticationMapper.toResponse(
                accessToken,
                null,
                snapshot,
                switched.activeTenant(),
                switched.availableTenants(),
                switched.activeContext(),
                switched.availableContexts()
        );
    }

    @PostMapping("/mobile/tenants/select")
    public LoginRes.MobileResponse mobileSelectTenant(@Valid @RequestBody LoginReq.SelectTenantReq request) {
        LoginRes res = selectTenant(request);
        return authenticationMapper.toMobileResponse(res, null);
    }

    @GetMapping("/account")
    public LoginRes account() {
        UUID userId = currentUserId();
        UUID tenantId = SecurityUtil.getCurrentTenantId().orElse(null);
        List<LoginRes.TenantRes> availableTenants = sessionService.tenants(userId);
        LoginRes.TenantRes activeTenant = tenantById(availableTenants, tenantId);
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId, tenantId);
        List<LoginRes.UserContextRes> contexts = tenantId == null
                ? List.of()
                : TenantContext.runWithTenant(tenantId, () -> sessionService.contexts(userId, tenantId));
        LoginRes.UserContextRes active = contextById(contexts,
                SecurityUtil.getCurrentActiveUserPersonId().orElse(null));
        return authenticationMapper.toResponse(null, null, snapshot, activeTenant, availableTenants, active, contexts);
    }

    @GetMapping("/contexts")
    public List<LoginRes.UserContextRes> contexts() {
        UUID tenantId = TenantContext.requireTenantId();
        return sessionService.contexts(currentUserId(), tenantId);
    }

    @PostMapping("/switch-context")
    public LoginRes switchContext(@Valid @RequestBody LoginReq.SwitchContextReq request) {
        UUID userId = currentUserId();
        UUID sessionId = currentSessionId();
        UUID tenantId = TenantContext.requireTenantId();
        AuthSessionService.ContextSwitchResult switched =
                sessionService.switchContext(userId, sessionId, request.getUserPersonId());
        List<LoginRes.TenantRes> availableTenants = sessionService.tenants(userId);
        LoginRes.TenantRes activeTenant = tenantById(availableTenants, tenantId);
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId, tenantId);
        return authenticationMapper.toResponse(
                securityUtil.createAccessToken(sessionId, snapshot, request.getUserPersonId()),
                null,
                snapshot,
                activeTenant,
                availableTenants,
                switched.activeContext(),
                switched.availableContexts()
        );
    }

    @GetMapping("/sessions")
    public List<SessionRes> sessions() {
        return sessionService.sessions(currentUserId()).stream()
                .sorted(Comparator.comparing(AuthSession::getCreatedAt).reversed())
                .map(SessionRes::from).toList();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String rawToken) {
        sessionService.revokeByRawToken(rawToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    @PostMapping("/mobile/logout")
    public void mobileLogout(@Valid @RequestBody LoginReq.RefreshTokenRequest request) {
        sessionService.revokeByRawToken(request.getRefreshToken());
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        sessionService.revokeAll(currentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    @PostMapping("/update-fcm")
    public void updateFcm(@Valid @RequestBody LoginReq.UpdateFcmReq request) {
        sessionService.updateFcm(currentSessionId(), request.getFcmToken(), request.getPlatform());
    }

    @PostMapping("/password-reset/options")
    public PasswordRecoveryReq.Options passwordResetOptions(@Valid @RequestBody PasswordRecoveryReq.Start request) {
        return passwordRecoveryService.options(request.phoneNumber());
    }

    @PostMapping("/password-reset/send")
    public PasswordRecoveryReq.Sent sendPasswordResetCode(@Valid @RequestBody PasswordRecoveryReq.Send request) {
        return passwordRecoveryService.send(request);
    }

    @PostMapping("/password-reset/verify")
    public PasswordRecoveryReq.Verified verifyPasswordResetCode(@Valid @RequestBody PasswordRecoveryReq.Verify request) {
        return passwordRecoveryService.verify(request);
    }

    @PostMapping("/password-reset/complete")
    public void completePasswordReset(@Valid @RequestBody PasswordRecoveryReq.Complete request) {
        passwordRecoveryService.complete(request, userService);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordReq request) {
        UUID userId = currentUserId();
        userService.changePassword(userId, request);
        sessionService.revokeAll(userId);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    @GetMapping("/recovery-emails")
    public List<PasswordRecoveryReq.EmailVerification> recoveryEmails() {
        return passwordRecoveryService.recoveryEmails(currentUserId());
    }

    @PostMapping("/recovery-emails/{userPersonId}/send")
    public PasswordRecoveryReq.Sent sendRecoveryEmailVerification(@PathVariable UUID userPersonId) {
        return passwordRecoveryService.sendEmailVerification(currentUserId(), userPersonId);
    }

    @PostMapping("/recovery-emails/{userPersonId}/verify")
    public void verifyRecoveryEmail(@PathVariable UUID userPersonId,
                                    @Valid @RequestBody PasswordRecoveryReq.EmailVerificationCode request) {
        passwordRecoveryService.verifyEmail(currentUserId(), userPersonId, request.code());
    }

    private LoginBundle authenticate(LoginReq.UserBase request, String platform) {
        Authentication authenticated;
        try {
            authenticated = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    PhoneNumberUtil.normalize(request.getPhoneNumber()), request.getPassword()));
        } catch (BadCredentialsException | DisabledException | LockedException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid phone number or password");
        }
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authenticated.getPrincipal();
        if (principal.getStatus() != UserStatus.ACTIVE) throw new DisabledException("User is not active");

        UUID userId = principal.getUserId();
        List<LoginRes.TenantRes> availableTenants = sessionService.tenants(userId);
        if (availableTenants.isEmpty()) {
            throw new ApiException(SecurityErrorCode.TENANT_ACCESS_DENIED);
        }

        LoginRes.TenantRes activeTenant = availableTenants.size() == 1 ? availableTenants.getFirst() : null;
        UUID selectedTenantId = activeTenant == null ? null : activeTenant.tenantId();

        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId, selectedTenantId);
        List<LoginRes.UserContextRes> contexts = selectedTenantId == null
                ? List.of()
                : TenantContext.runWithTenant(selectedTenantId, () -> sessionService.contexts(userId, selectedTenantId));
        LoginRes.UserContextRes active = contexts.size() == 1 ? contexts.getFirst() : null;

        String rawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthSession session = sessionService.create(
                userId,
                selectedTenantId,
                rawRefreshToken,
                request.getIdDevice(),
                platform,
                request.getFcmToken(),
                active == null ? null : active.userPersonId());
        userService.updateLastLogin(userId);
        String accessToken = securityUtil.createAccessToken(
                session.getAuthSessionId(),
                snapshot,
                active == null ? null : active.userPersonId());
        return new LoginBundle(
                authenticationMapper.toResponse(
                        accessToken,
                        request.getIdDevice(),
                        snapshot,
                        activeTenant,
                        availableTenants,
                        active,
                        contexts),
                rawRefreshToken);
    }

    private LoginBundle refreshBundle(String currentRawToken) {
        if (currentRawToken == null || currentRawToken.isBlank()) {
            throw new ApiException(SecurityErrorCode.MISSING_REFRESH_TOKEN);
        }
        String nextRawToken = RefreshTokenUtil.generateRawToken();
        AuthSession session = sessionService.rotate(currentRawToken, nextRawToken);
        UUID userId = session.getUser().getUserId();
        UUID tenantId = session.getTenantId();

        List<LoginRes.TenantRes> availableTenants = sessionService.tenants(userId);
        LoginRes.TenantRes activeTenant = tenantById(availableTenants, tenantId);
        if (tenantId != null && activeTenant == null) {
            sessionService.revoke(session.getAuthSessionId());
            throw new ApiException(SecurityErrorCode.TENANT_MEMBERSHIP_REVOKED);
        }

        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId, tenantId);
        if (snapshot.userStatus() != UserStatus.ACTIVE) {
            sessionService.revoke(session.getAuthSessionId());
            throw new ApiException(SecurityErrorCode.USER_NOT_ACTIVE);
        }

        List<LoginRes.UserContextRes> contexts = tenantId == null
                ? List.of()
                : TenantContext.runWithTenant(tenantId, () -> sessionService.contexts(userId, tenantId));
        UUID activeId = session.getActiveUserPerson() == null
                ? null : session.getActiveUserPerson().getUserPersonId();
        LoginRes.UserContextRes active = contextById(contexts, activeId);
        if (activeId != null && active == null) {
            sessionService.revoke(session.getAuthSessionId());
            throw new ApiException(SecurityErrorCode.ACTIVE_CONTEXT_UNAVAILABLE);
        }
        String accessToken = securityUtil.createAccessToken(session.getAuthSessionId(), snapshot, activeId);
        return new LoginBundle(
                authenticationMapper.toResponse(
                        accessToken,
                        session.getDeviceInfo(),
                        snapshot,
                        activeTenant,
                        availableTenants,
                        active,
                        contexts),
                nextRawToken);
    }

    private LoginRes.TenantRes tenantById(List<LoginRes.TenantRes> tenants, UUID id) {
        if (id == null) return null;
        return tenants.stream().filter(value -> value.tenantId().equals(id)).findFirst().orElse(null);
    }

    private LoginRes.UserContextRes contextById(List<LoginRes.UserContextRes> contexts, UUID id) {
        if (id == null) return null;
        return contexts.stream().filter(value -> value.userPersonId().equals(id)).findFirst().orElse(null);
    }

    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Missing user"));
    }

    private UUID currentSessionId() {
        return SecurityUtil.getCurrentSessionId().orElseThrow(() -> new AccessDeniedException("Missing session"));
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(refreshCookieSecure)
                .path("/api/v1/auth").sameSite(refreshCookieSameSite)
                .maxAge(refreshTokenExpiration).build();
    }

    private ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "").httpOnly(true).secure(refreshCookieSecure)
                .path("/api/v1/auth").sameSite(refreshCookieSameSite).maxAge(0).build();
    }

    private record LoginBundle(LoginRes response, String refreshToken) {
    }

    public record SessionRes(UUID authSessionId, String deviceInfo, String platform, boolean revoked,
                             LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt,
                             UUID tenantId, UUID activeUserPersonId) {
        static SessionRes from(AuthSession session) {
            return new SessionRes(session.getAuthSessionId(), session.getDeviceInfo(), session.getPlatform(),
                    session.isRevoked(), session.getCreatedAt(), session.getUpdatedAt(), session.getExpiresAt(),
                    session.getTenantId(),
                    session.getActiveUserPerson() == null ? null
                            : session.getActiveUserPerson().getUserPersonId());
        }
    }
}
