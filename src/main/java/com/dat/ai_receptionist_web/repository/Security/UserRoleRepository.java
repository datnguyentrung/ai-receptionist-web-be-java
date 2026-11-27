package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {
    @Query(value = """
            select ur
            from UserRole ur
            join fetch ur.user
            join fetch ur.role
            where ur.id.tenantId = :tenantId
            """,
            countQuery = """
            select count(ur)
            from UserRole ur
            where ur.id.tenantId = :tenantId
            """)
    Page<UserRole> findAllDetailedByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = """
            select ur
            from UserRole ur
            join fetch ur.user
            join fetch ur.role
            where ur.id.tenantId = :tenantId
              and ur.id.roleId <> :excludedRoleCode
            """,
            countQuery = """
            select count(ur)
            from UserRole ur
            where ur.id.tenantId = :tenantId
              and ur.id.roleId <> :excludedRoleCode
            """)
    Page<UserRole> findAllDetailedByTenantIdAndRoleCodeNot(
            @Param("tenantId") UUID tenantId,
            @Param("excludedRoleCode") String excludedRoleCode,
            Pageable pageable);

    List<UserRole> findAllById_TenantIdAndId_UserId(UUID tenantId, UUID userId);

    @EntityGraph(attributePaths = "role")
    @Query("""
            select ur
            from UserRole ur
            where ur.id.tenantId = :tenantId
              and ur.user.userId = :userId
            order by ur.role.code
            """)
    List<UserRole> findAllDetailedByTenantIdAndUserId(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId);

    @Query("select ur.role.code from UserRole ur where ur.id.tenantId = :tenantId and ur.id.userId = :userId order by ur.role.code")
    SortedSet<String> findRoleCodes(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("""
            select ur.user.userId as userId, ur.role.code as roleCode
            from UserRole ur
            where ur.id.tenantId = :tenantId
              and ur.user.userId in :userIds
            order by ur.user.userId, ur.role.code
            """)
    List<UserRoleRow> findAllByTenantIdAndUserIdIn(
            @Param("tenantId") UUID tenantId,
            @Param("userIds") Set<UUID> userIds);

    void deleteById_TenantIdAndId_UserIdAndId_RoleIdIn(
            UUID tenantId,
            UUID userId,
            Set<String> roleCodes
    );

    void deleteAllById_TenantIdAndId_UserId(UUID tenantId, UUID userId);

    interface UserRoleRow {
        UUID getUserId();
        String getRoleCode();
    }
}
