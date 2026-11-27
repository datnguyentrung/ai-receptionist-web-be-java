package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformOwnerService {
    public static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";

    private final JdbcTemplate jdbcTemplate;

    public Optional<UUID> findPlatformOwnerUserId() {
        return jdbcTemplate.query(
                "SELECT user_id FROM security.platform_owner WHERE singleton_key = TRUE",
                rs -> rs.next() ? Optional.ofNullable(rs.getObject("user_id", UUID.class)) : Optional.empty()
        );
    }

    public boolean isPlatformOwner(UUID userId) {
        if (userId == null) {
            return false;
        }
        return findPlatformOwnerUserId().map(userId::equals).orElse(false);
    }

    public boolean isCurrentUserPlatformOwner() {
        return SecurityUtil.getCurrentUserId().map(this::isPlatformOwner).orElse(false);
    }

    public void requirePlatformOwner() {
        UUID currentUserId = SecurityUtil.getCurrentUserId()
                .orElseThrow(() -> new ApiException(SecurityErrorCode.ACCESS_DENIED));
        if (!isPlatformOwner(currentUserId)) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "Platform owner access required");
        }
    }
}
