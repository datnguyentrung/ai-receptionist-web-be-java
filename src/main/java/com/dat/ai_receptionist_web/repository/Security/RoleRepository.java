package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.*;

public interface RoleRepository extends JpaRepository<Role, Role.Key> {
    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    List<Role> findAllByTenantIdAndCodeIn(UUID tenantId, Collection<String> codes);

    Page<Role> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<Role> findAllByTenantIdAndCodeNot(UUID tenantId, String excludedCode, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.tenantId = :tenantId and r.code = :roleCode")
    Optional<Role> findByTenantIdAndCodeForUpdate(@Param("tenantId") UUID tenantId, @Param("roleCode") String roleCode);

    @Query("""
            select rp.role.code as roleCode,
                   p.permissionId as permissionId,
                   p.code as permissionCode,
                   p.model as permissionModel,
                   p.action as permissionAction,
                   p.description as permissionDescription
            from RolePermission rp
            join rp.permission p
            where rp.id.tenantId = :tenantId
              and rp.role.code in :roleCodes
            order by rp.role.code, p.code
            """)
    List<RolePermissionRow> findPermissionsByTenantIdAndRoleCodeIn(
            @Param("tenantId") UUID tenantId,
            @Param("roleCodes") Set<String> roleCodes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Role r set r.permissionVersion = r.permissionVersion + 1 where r.tenantId = :tenantId and r.code = :roleCode")
    int incrementPermissionVersion(@Param("tenantId") UUID tenantId, @Param("roleCode") String roleCode);

    interface RolePermissionRow {
        String getRoleCode();
        Integer getPermissionId();
        String getPermissionCode();
        String getPermissionModel();
        PermissionAction getPermissionAction();
        String getPermissionDescription();
    }
}
