package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findAllByPhoneNumberIn(Set<String> phoneNumbers);

    @Query(value = """
            select u
            from User u
            where (:phoneSearch <> '' and lower(u.phoneNumber) like concat('%', :phoneSearch, '%'))
               or exists (
                   select 1
                   from UserPerson up
                   join up.person p
                   where up.user = u
                     and up.active = true
                     and lower(p.fullName) like concat('%', :search, '%')
               )
            """,
            countQuery = """
            select count(u)
            from User u
            where (:phoneSearch <> '' and lower(u.phoneNumber) like concat('%', :phoneSearch, '%'))
               or exists (
                   select 1
                   from UserPerson up
                   join up.person p
                   where up.user = u
                     and up.active = true
                     and lower(p.fullName) like concat('%', :search, '%')
               )
            """)
    Page<User> searchByPhoneNumberOrPersonFullName(
            @Param("search") String search,
            @Param("phoneSearch") String phoneSearch,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId in :userIds")
    List<User> findAllByUserIdInForUpdate(@Param("userIds") Set<UUID> userIds);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.lastLoginAt = current_timestamp where u.userId = :userId")
    int updateLastLogin(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.authorizationVersion = u.authorizationVersion + 1 where u.userId = :userId")
    int incrementAuthorizationVersion(@Param("userId") UUID userId);

    @Query(value = """
            SELECT u.user_id AS userId,
                   u.phone_number AS phoneNumber,
                   u.user_status AS userStatus,
                   u.authorization_version AS authorizationVersion,
                   r.role_id AS roleCode,
                   r.permission_version AS permissionVersion,
                   p.code AS permissionCode
            FROM security.users u
            LEFT JOIN security.user_role ur ON ur.user_id = u.user_id
            LEFT JOIN security.role r ON r.role_id = ur.role_id
            LEFT JOIN security.role_permission rp ON rp.role_id = r.role_id
            LEFT JOIN security.permission p ON p.permission_id = rp.permission_id
            WHERE u.user_id = :userId
            ORDER BY r.role_id, p.code
            """, nativeQuery = true)
    List<AuthorizationRow> findAuthorizationRows(@Param("userId") UUID userId);

    @Query(value = """
            SELECT DISTINCT ur.user_id
            FROM security.user_role ur
            WHERE ur.role_id = :roleCode
            """, nativeQuery = true)
    List<UUID> findUserIdsByRoleCode(@Param("roleCode") String roleCode);

    interface AuthorizationRow {
        UUID getUserId();
        String getPhoneNumber();
        String getUserStatus();
        long getAuthorizationVersion();
        String getRoleCode();
        Long getPermissionVersion();
        String getPermissionCode();
    }
}
