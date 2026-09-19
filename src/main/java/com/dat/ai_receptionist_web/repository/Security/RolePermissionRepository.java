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
            """,
            countQuery = """
            select count(rp)
            from RolePermission rp
            """)
    Page<RolePermission> findAllDetailed(Pageable pageable);

    List<RolePermission> findAllById_RoleId(String roleId);

    @Query("select rp.permission.code from RolePermission rp where rp.id.roleId = :role order by rp.permission.code")
    SortedSet<String> findPermissionCodes(@Param("role") String roleCode);

    @Query("""
            select rp.role.code as roleCode, rp.permission.code as permissionCode
            from RolePermission rp
            where rp.role.code in :roleCodes
            order by rp.role.code, rp.permission.code
            """)
    List<RolePermissionCodeRow> findPermissionCodeRowsByRoleCodeIn(@Param("roleCodes") Set<String> roleCodes);

    @Modifying
    @Query("""
            delete from RolePermission rp
            where rp.role.code = :roleCode
              and rp.permission.code in :permissionCodes
            """)
    void deleteByRoleCodeAndPermissionCodeIn(String roleCode, Set<String> toRemove);

    @Query("""
            select distinct rp.role.code
            from RolePermission rp
            where rp.permission.code in :permissionCodes
            """)
    Set<String> findRoleCodesByPermissionCodeIn(@Param("permissionCodes") Set<String> permissionCodes);

    @Modifying
    @Query("""
            delete from RolePermission rp
            where rp.permission.code in :permissionCodes
            """)
    int deleteByPermissionCodeIn(@Param("permissionCodes") Set<String> permissionCodes);

    interface RolePermissionCodeRow {
        String getRoleCode();
        String getPermissionCode();
    }
}
