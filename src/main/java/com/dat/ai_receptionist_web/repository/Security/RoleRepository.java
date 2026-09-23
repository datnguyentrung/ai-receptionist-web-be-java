package com.dat.ai_receptionist_web.repository.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.*;

public interface RoleRepository extends JpaRepository<Role, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Role r where r.code = :roleCode")
    Optional<Role> findByIdForUpdate(@Param("roleCode") String roleCode);

    @Query("""
            select rp.role.code as roleCode,
                   p.permissionId as permissionId,
                   p.code as permissionCode,
                   p.model as permissionModel,
                   p.action as permissionAction,
                   p.description as permissionDescription
            from RolePermission rp
            join rp.permission p
            where rp.role.code in :roleCodes
            order by rp.role.code, p.code
            """)
    List<RolePermissionRow> findPermissionsByRoleCodeIn(@Param("roleCodes") Set<String> roleCodes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Role r set r.permissionVersion = r.permissionVersion + 1 where r.code = :roleCode")
    int incrementPermissionVersion(@Param("roleCode") String roleCode);

    interface RolePermissionRow {
        String getRoleCode();
        Integer getPermissionId();
        String getPermissionCode();
        String getPermissionModel();
        PermissionAction getPermissionAction();
        String getPermissionDescription();
    }
}
