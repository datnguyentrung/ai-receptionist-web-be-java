package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.AuthSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

    @Query(value = """
            select s
            from AuthSession s
            join fetch s.user
            left join fetch s.activeUserPerson activeUserPerson
            left join fetch activeUserPerson.user
            left join fetch activeUserPerson.person
            """,
            countQuery = """
            select count(s)
            from AuthSession s
            """)
    Page<AuthSession> findAllDetailed(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s join fetch s.user left join fetch s.activeUserPerson where s.refreshTokenHash = :hash")
    Optional<AuthSession> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    @EntityGraph(attributePaths = "activeUserPerson")
    List<AuthSession> findAllByUser_UserId(UUID userId);
    List<AuthSession> findAllByUser_UserIdAndRevokedFalse(UUID userId);
    List<AuthSession> findAllByActiveUserPerson_Person_PersonIdAndRevokedFalse(UUID personId);
    Optional<AuthSession> findByFcmToken(String fcmToken);

    @Query(value = """
            SELECT s.revoked AS revoked,
                   s.expires_at AS expiresAt,
                   s.active_user_person_id AS activeUserPersonId,
                   CASE
                       WHEN s.active_user_person_id IS NULL THEN TRUE
                       WHEN up.active = TRUE AND up.user_id = s.user_id THEN TRUE
                       ELSE FALSE
                   END AS activeUserPersonActive,
                   u.user_status AS userStatus,
                   u.authorization_version AS authorizationVersion,
                   r.role_id AS roleCode,
                   r.permission_version AS permissionVersion
            FROM security.auth_session s
            JOIN security.users u ON u.user_id = s.user_id
            LEFT JOIN core.user_person up ON up.user_person_id = s.active_user_person_id
            LEFT JOIN security.user_role ur ON ur.user_id = u.user_id
            LEFT JOIN security.role r ON r.role_id = ur.role_id
            WHERE s.auth_session_id = :sessionId
              AND s.user_id = :userId
            ORDER BY r.role_id
            """, nativeQuery = true)
    List<AccessStateRow> findAccessState(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId);

    interface AccessStateRow {
        boolean getRevoked();
        LocalDateTime getExpiresAt();
        UUID getActiveUserPersonId();
        boolean getActiveUserPersonActive();
        String getUserStatus();
        long getAuthorizationVersion();
        String getRoleCode();
        Long getPermissionVersion();
    }
}
