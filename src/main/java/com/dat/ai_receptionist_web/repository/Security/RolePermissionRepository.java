package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Key> {
    @Query(value = """
            select rp
            from RolePermission rp
            join fetch rp.role
            join fetch rp.permission
            where rp.id.tenantId = :tenantId
            """,
            countQuery = """
            select count(rp)
            from RolePermission rp
            where rp.id.tenantId = :tenantId
            """)
    Page<RolePermission> findAllDetailedByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query(value = """
            select rp
            from RolePermission rp
            join fetch rp.role
            join fetch rp.permission
            where rp.id.tenantId = :tenantId
              and rp.id.roleId <> :excludedRoleCode
            """,
            countQuery = """
            select count(rp)
            from RolePermission rp
            where rp.id.tenantId = :tenantId
              and rp.id.roleId <> :excludedRoleCode
            """)
    Page<RolePermission> findAllDetailedByTenantIdAndRoleCodeNot(
            @Param("tenantId") UUID tenantId,
            @Param("excludedRoleCode") String excludedRoleCode,
            Pageable pageable);

    List<RolePermission> findAllById_TenantIdAndId_RoleId(UUID tenantId, String roleId);

    @Query("select rp.permission.code from RolePermission rp where rp.id.tenantId = :tenantId and rp.id.roleId = :role order by rp.permission.code")
    SortedSet<String> findPermissionCodes(@Param("tenantId") UUID tenantId, @Param("role") String roleCode);

    @Query("""
            select rp.role.code as roleCode, rp.permission.code as permissionCode
            from RolePermission rp
            where rp.id.tenantId = :tenantId
              and rp.role.code in :roleCodes
            order by rp.role.code, rp.permission.code
            """)
    List<RolePermissionCodeRow> findPermissionCodeRowsByRoleCodeIn(
            @Param("tenantId") UUID tenantId,
            @Param("roleCodes") Set<String> roleCodes);

    @Modifying
    @Query("""
            delete from RolePermission rp
            where rp.id.tenantId = :tenantId
              and rp.id.roleId = :roleCode
              and rp.permission.code in :permissionCodes
            """)
    void deleteByTenantIdAndRoleCodeAndPermissionCodeIn(
            @Param("tenantId") UUID tenantId,
            @Param("roleCode") String roleCode,
            @Param("permissionCodes") Set<String> toRemove);

    @Query("""
            select distinct rp.id.roleId
            from RolePermission rp
            where rp.id.tenantId = :tenantId
              and rp.permission.code in :permissionCodes
            """)
    Set<String> findRoleCodesByTenantIdAndPermissionCodeIn(
            @Param("tenantId") UUID tenantId,
            @Param("permissionCodes") Set<String> permissionCodes);

    @Modifying
    @Query("""
            delete from RolePermission rp
            where rp.permission.code in :permissionCodes
            """)
    int deleteByPermissionCodeIn(@Param("permissionCodes") Set<String> permissionCodes);

    @Modifying
    @Query(value = """
            UPDATE security.role r
            SET permission_version = permission_version + 1
            WHERE EXISTS (
                SELECT 1
                FROM security.role_permission rp
                JOIN security.permission p ON p.permission_id = rp.permission_id
                WHERE rp.tenant_id = r.tenant_id
                  AND rp.role_id = r.role_id
                  AND p.code IN (:permissionCodes)
            )
            """, nativeQuery = true)
    int incrementAffectedRoleVersionsByPermissionCodeIn(@Param("permissionCodes") Set<String> permissionCodes);

    interface RolePermissionCodeRow {
        String getRoleCode();
        String getPermissionCode();
    }
}
