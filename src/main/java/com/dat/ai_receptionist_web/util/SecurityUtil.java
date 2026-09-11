package com.dat.ai_receptionist_web.util;

import com.dat.ai_receptionist_web.service.Security.AuthorizationSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class SecurityUtil {
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiration;

    public SecurityUtil(JwtEncoder jwtEncoder,
                        @Value("${jwt.access-token-validity-in-seconds}") long accessTokenExpiration) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String createAccessToken(UUID authSessionId, AuthorizationSnapshot snapshot,
                                    UUID activeUserPersonId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiration))
                .subject(snapshot.userId().toString())
                .claim("userId", snapshot.userId().toString())
                .claim("authSessionId", authSessionId.toString())
                .claim("authorizationVersion", snapshot.authorizationVersion())
                .claim("roles", snapshot.roleCodes())
                .claim("rolePermissionVersions", snapshot.rolePermissionVersions())
                .claim("permissions", snapshot.permissionCodes());
        if (activeUserPersonId != null) {
            claims.claim("activeUserPersonId", activeUserPersonId.toString());
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(JWT_ALGORITHM).build(), claims.build())).getTokenValue();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getJwt().map(Jwt::getSubject).map(UUID::fromString);
    }

    public static Optional<UUID> getCurrentSessionId() {
        return getJwt().map(jwt -> jwt.getClaimAsString("authSessionId")).map(UUID::fromString);
    }

    public static Optional<UUID> getCurrentActiveUserPersonId() {
        return getJwt().map(jwt -> jwt.getClaimAsString("activeUserPersonId"))
                .filter(Objects::nonNull).map(UUID::fromString);
    }

    public static Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Jwt jwt
                ? Optional.of(jwt) : Optional.empty();
    }

    public static Optional<String> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return Optional.empty();
        if (authentication.getPrincipal() instanceof Jwt jwt) return Optional.of(jwt.getSubject());
        if (authentication.getPrincipal() instanceof UserDetails user) return Optional.of(user.getUsername());
        return Optional.empty();
    }
}
