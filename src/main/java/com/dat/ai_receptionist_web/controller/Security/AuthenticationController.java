package com.dat.ai_receptionist_web.controller.Security;

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

    @Value("${jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;
    @Value("${auth.refresh-cookie.secure:true}")
    private boolean refreshCookieSecure;
    @Value("${auth.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    /**
     * Tác dụng: Thực hiện logic login của lớp hiện tại.
     * Input: Nhận LoginReq.UserBase request từ caller hoặc request.
     * Output: Trả về ResponseEntity<LoginRes> theo kết quả xử lý.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq.UserBase request) {
        LoginBundle bundle = authenticate(request, "WEB");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(bundle.refreshToken()).toString())
                .body(bundle.response());
    }

    /**
     * Tác dụng: Thực hiện logic mobileLogin của lớp hiện tại.
     * Input: Nhận LoginReq.MobileLoginRequest request từ caller hoặc request.
     * Output: Trả về ResponseEntity<LoginRes.MobileResponse> theo kết quả xử lý.
     */
    @PostMapping("/mobile/login")
    public ResponseEntity<LoginRes.MobileResponse> mobileLogin(
            @Valid @RequestBody LoginReq.MobileLoginRequest request) {
        LoginBundle bundle = authenticate(request, request.getPlatform().toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(authenticationMapper.toMobileResponse(bundle.response(), bundle.refreshToken()));
    }

    /**
     * Tác dụng: Thực hiện logic refresh của lớp hiện tại.
     * Input: Nhận String rawToken từ caller hoặc request.
     * Output: Trả về ResponseEntity<LoginRes> theo kết quả xử lý.
     */
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

    /**
     * Tác dụng: Thực hiện logic mobileRefresh của lớp hiện tại.
     * Input: Nhận LoginReq.RefreshTokenRequest request từ caller hoặc request.
     * Output: Trả về ResponseEntity<LoginRes.MobileResponse> theo kết quả xử lý.
     */
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

    /**
     * Tác dụng: Thực hiện logic account của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về LoginRes theo kết quả xử lý.
     */
    @GetMapping("/account")
    public LoginRes account() {
        UUID userId = currentUserId();
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        UUID sessionId = currentSessionId();
        List<LoginRes.UserContextRes> contexts = sessionService.contexts(userId);
        LoginRes.UserContextRes active = contextById(contexts,
                SecurityUtil.getCurrentActiveUserPersonId().orElse(null));
        return authenticationMapper.toResponse(null, null, snapshot, active, contexts);
    }

    /**
     * Tác dụng: Thực hiện logic contexts của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về List<LoginRes.UserContextRes> theo kết quả xử lý.
     */
    @GetMapping("/contexts")
    public List<LoginRes.UserContextRes> contexts() {
        return sessionService.contexts(currentUserId());
    }

    /**
     * Tác dụng: Chuyển ngữ cảnh hoạt động của người dùng sau khi kiểm tra quyền sở hữu.
     * Input: Nhận LoginReq.SwitchContextReq request từ caller hoặc request.
     * Output: Trả về LoginRes theo kết quả xử lý.
     */
    @PostMapping("/switch-context")
    public LoginRes switchContext(@Valid @RequestBody LoginReq.SwitchContextReq request) {
        UUID userId = currentUserId();
        UUID sessionId = currentSessionId();
        AuthSessionService.ContextSwitchResult switched =
                sessionService.switchContext(userId, sessionId, request.getUserPersonId());
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        return authenticationMapper.toResponse(securityUtil.createAccessToken(sessionId, snapshot, request.getUserPersonId()),
                null, snapshot, switched.activeContext(), switched.availableContexts());
    }

    /**
     * Tác dụng: Thực hiện logic sessions của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về List<SessionRes> theo kết quả xử lý.
     */
    @GetMapping("/sessions")
    public List<SessionRes> sessions() {
        return sessionService.sessions(currentUserId()).stream()
                .sorted(Comparator.comparing(AuthSession::getCreatedAt).reversed())
                .map(SessionRes::from).toList();
    }

    /**
     * Tác dụng: Thực hiện logic logout của lớp hiện tại.
     * Input: Nhận String rawToken từ caller hoặc request.
     * Output: Trả về ResponseEntity<Void> theo kết quả xử lý.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String rawToken) {
        sessionService.revokeByRawToken(rawToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    /**
     * Tác dụng: Thực hiện logic mobileLogout của lớp hiện tại.
     * Input: Nhận LoginReq.RefreshTokenRequest request từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @PostMapping("/mobile/logout")
    public void mobileLogout(@Valid @RequestBody LoginReq.RefreshTokenRequest request) {
        sessionService.revokeByRawToken(request.getRefreshToken());
    }

    /**
     * Tác dụng: Thực hiện logic logoutAll của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về ResponseEntity<Void> theo kết quả xử lý.
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        sessionService.revokeAll(currentUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteRefreshCookie().toString()).build();
    }

    /**
     * Tác dụng: Thực hiện logic updateFcm của lớp hiện tại.
     * Input: Nhận LoginReq.UpdateFcmReq request từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @PostMapping("/update-fcm")
    public void updateFcm(@Valid @RequestBody LoginReq.UpdateFcmReq request) {
        sessionService.updateFcm(currentSessionId(), request.getFcmToken(), request.getPlatform());
    }

    /**
     * Tác dụng: Thực hiện logic authenticate của lớp hiện tại.
     * Input: Nhận LoginReq.UserBase request, String platform từ caller hoặc request.
     * Output: Trả về LoginBundle theo kết quả xử lý.
     */
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
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        List<LoginRes.UserContextRes> contexts = sessionService.contexts(userId);
        LoginRes.UserContextRes active = contexts.size() == 1 ? contexts.getFirst() : null;
        String rawRefreshToken = RefreshTokenUtil.generateRawToken();
        AuthSession session = sessionService.create(userId, rawRefreshToken, request.getIdDevice(),
                platform, request.getFcmToken(), active == null ? null : active.userPersonId());
        userService.updateLastLogin(userId);
        String accessToken = securityUtil.createAccessToken(session.getAuthSessionId(), snapshot,
                active == null ? null : active.userPersonId());
        return new LoginBundle(authenticationMapper.toResponse(accessToken, request.getIdDevice(), snapshot, active, contexts),
                rawRefreshToken);
    }

    /**
     * Tác dụng: Thực hiện logic refreshBundle của lớp hiện tại.
     * Input: Nhận String currentRawToken từ caller hoặc request.
     * Output: Trả về LoginBundle theo kết quả xử lý.
     */
    private LoginBundle refreshBundle(String currentRawToken) {
        if (currentRawToken == null || currentRawToken.isBlank()) {
            throw new ApiException(SecurityErrorCode.MISSING_REFRESH_TOKEN);
        }
        String nextRawToken = RefreshTokenUtil.generateRawToken();
        AuthSession session = sessionService.rotate(currentRawToken, nextRawToken);
        UUID userId = session.getUser().getUserId();
        AuthorizationSnapshot snapshot = authorizationService.loadSnapshot(userId);
        if (snapshot.userStatus() != UserStatus.ACTIVE) {
            sessionService.revoke(session.getAuthSessionId());
            throw new ApiException(SecurityErrorCode.USER_NOT_ACTIVE);
        }
        List<LoginRes.UserContextRes> contexts = sessionService.contexts(userId);
        UUID activeId = session.getActiveUserPerson() == null
                ? null : session.getActiveUserPerson().getUserPersonId();
        LoginRes.UserContextRes active = contextById(contexts, activeId);
        if (activeId != null && active == null) {
            sessionService.revoke(session.getAuthSessionId());
            throw new ApiException(SecurityErrorCode.ACTIVE_CONTEXT_UNAVAILABLE);
        }
        String accessToken = securityUtil.createAccessToken(session.getAuthSessionId(), snapshot, activeId);
        return new LoginBundle(authenticationMapper.toResponse(accessToken, session.getDeviceInfo(), snapshot, active, contexts),
                nextRawToken);
    }

    /**
     * Tác dụng: Thực hiện logic contextById của lớp hiện tại.
     * Input: Nhận List<LoginRes.UserContextRes> contexts, UUID id từ caller hoặc request.
     * Output: Trả về LoginRes.UserContextRes theo kết quả xử lý.
     */
    private LoginRes.UserContextRes contextById(List<LoginRes.UserContextRes> contexts, UUID id) {
        if (id == null) return null;
        return contexts.stream().filter(value -> value.userPersonId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Tác dụng: Lấy thông tin hiện tại từ ngữ cảnh bảo mật của request đang xử lý.
     * Input: Không có tham số đầu vào.
     * Output: Trả về UUID theo kết quả xử lý.
     */
    private UUID currentUserId() {
        return SecurityUtil.getCurrentUserId().orElseThrow(() -> new AccessDeniedException("Missing user"));
    }

    /**
     * Tác dụng: Lấy thông tin hiện tại từ ngữ cảnh bảo mật của request đang xử lý.
     * Input: Không có tham số đầu vào.
     * Output: Trả về UUID theo kết quả xử lý.
     */
    private UUID currentSessionId() {
        return SecurityUtil.getCurrentSessionId().orElseThrow(() -> new AccessDeniedException("Missing session"));
    }

    /**
     * Tác dụng: Thực hiện logic refreshCookie của lớp hiện tại.
     * Input: Nhận String value từ caller hoặc request.
     * Output: Trả về ResponseCookie theo kết quả xử lý.
     */
    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(refreshCookieSecure)
                .path("/api/v1/auth").sameSite(refreshCookieSameSite)
                .maxAge(refreshTokenExpiration).build();
    }

    /**
     * Tác dụng: Thực hiện logic deleteRefreshCookie của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về ResponseCookie theo kết quả xử lý.
     */
    private ResponseCookie deleteRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "").httpOnly(true).secure(refreshCookieSecure)
                .path("/api/v1/auth").sameSite(refreshCookieSameSite).maxAge(0).build();
    }

    private record LoginBundle(LoginRes response, String refreshToken) {
    }

    public record SessionRes(UUID authSessionId, String deviceInfo, String platform, boolean revoked,
                             LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime expiresAt,
                             UUID activeUserPersonId) {
        static SessionRes from(AuthSession session) {
            return new SessionRes(session.getAuthSessionId(), session.getDeviceInfo(), session.getPlatform(),
                    session.isRevoked(), session.getCreatedAt(), session.getUpdatedAt(), session.getExpiresAt(),
                    session.getActiveUserPerson() == null ? null
                            : session.getActiveUserPerson().getUserPersonId());
        }
    }
}


