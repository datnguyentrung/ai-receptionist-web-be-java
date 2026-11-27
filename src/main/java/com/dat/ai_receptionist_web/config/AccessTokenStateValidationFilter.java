package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.error.ApiErrorResponseFactory;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.service.Security.AuthorizationService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AccessTokenStateValidationFilter extends OncePerRequestFilter {
    private static final Set<String> TENANT_SELECTION_ALLOWED_PATHS = Set.of(
            "/api/v1/auth/tenants",
            "/api/v1/auth/tenants/select",
            "/api/v1/auth/mobile/tenants/select",
            "/api/v1/auth/account",
            "/api/v1/auth/logout",
            "/api/v1/auth/mobile/logout",
            "/api/v1/auth/logout-all",
            "/api/v1/auth/refresh",
            "/api/v1/auth/mobile/refresh"
    );

    private static final List<GrantedAuthority> INTERNAL_AGENT_READ_AUTHORITIES = Stream.of(
            PermissionDefinition.BRANCH_READ,
            PermissionDefinition.PERSON_READ,
            PermissionDefinition.POSITION_READ,
            PermissionDefinition.CLASS_SCHEDULE_READ,
            PermissionDefinition.COURSE_READ,
            PermissionDefinition.TRAINING_PLAN_READ,
            PermissionDefinition.TRAINING_PLAN_REQUIREMENT_READ,
            PermissionDefinition.TRAINING_PLAN_ACCESS_RULE_READ,
            PermissionDefinition.TRAINING_PLAN_ADDON_READ,
            PermissionDefinition.CLASS_SESSION_READ,
            PermissionDefinition.COURSE_STAFF_ASSIGNMENT_READ,
            PermissionDefinition.BELT_EXAM_READ,
            PermissionDefinition.KNOWLEDGE_READ
    ).map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getCode())).toList();

    private final AuthorizationService authorizationService;
    private final ApiErrorResponseFactory errorResponseFactory;

    @Value("${python-backend.internal-token:${PYTHON_INTERNAL_AUTH_TOKEN:${HUGGING_FACE_TOKEN:}}}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                    ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof Jwt jwt) {
                try {
                    authorizationService.validateAccessToken(jwt);
                } catch (RuntimeException exception) {
                    SecurityContextHolder.clearContext();
                    errorResponseFactory.write(response, SecurityErrorCode.TOKEN_STALE, request);
                    return;
                }

                UUID tenantId = SecurityUtil.getCurrentTenantId().orElse(null);
                if (tenantId == null) {
                    String uri = request.getRequestURI();
                    if (!TENANT_SELECTION_ALLOWED_PATHS.contains(uri)) {
                        errorResponseFactory.write(response, SecurityErrorCode.TENANT_SELECTION_REQUIRED, request);
                        return;
                    }
                } else {
                    TenantContext.setTenantId(tenantId);
                }
            } else if ("GET".equalsIgnoreCase(request.getMethod())) {
                tryAuthenticateInternalCatalogReader(request);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void tryAuthenticateInternalCatalogReader(HttpServletRequest request) {
        if (!StringUtils.hasText(internalToken)) {
            return;
        }
        String providedToken = request.getHeader("X-Internal-Service-Token");
        String rawTenantId = request.getHeader("X-Tenant-Id");
        if (!StringUtils.hasText(providedToken) || !StringUtils.hasText(rawTenantId)) {
            return;
        }
        boolean tokenMatches = MessageDigest.isEqual(
                internalToken.trim().getBytes(StandardCharsets.UTF_8),
                providedToken.trim().getBytes(StandardCharsets.UTF_8)
        );
        if (!tokenMatches) {
            return;
        }
        try {
            UUID tenantId = UUID.fromString(rawTenantId.trim());
            TenantContext.setTenantId(tenantId);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "internal-ai-agent",
                    null,
                    INTERNAL_AGENT_READ_AUTHORITIES
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (IllegalArgumentException ignored) {
            // Invalid tenant UUID header; leave unauthenticated.
        }
    }
}
