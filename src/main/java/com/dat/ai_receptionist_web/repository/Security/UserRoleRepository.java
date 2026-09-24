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
            """,
            countQuery = """
            select count(ur)
            from UserRole ur
            """)
    Page<UserRole> findAllDetailed(Pageable pageable);

    List<UserRole> findAllById_UserId(UUID userId);

    @EntityGraph(attributePaths = "role")
    @Query("""
            select ur
            from UserRole ur
            where ur.user.userId = :userId
            order by ur.role.code
            """)
    List<UserRole> findAllDetailedByUserId(@Param("userId") UUID userId);

    @Query("select ur.role.code from UserRole ur where ur.id.userId = :userId order by ur.role.code")
    SortedSet<String> findRoleCodes(@Param("userId") UUID userId);

    @Query("""
            select ur.user.userId as userId, ur.role.code as roleCode
            from UserRole ur
            where ur.user.userId in :userIds
            order by ur.user.userId, ur.role.code
            """)
    List<UserRoleRow> findAllByUserIdIn(@Param("userIds") Set<UUID> userIds);

    void deleteById_UserIdAndRole_CodeIn(
            UUID userId,
            Set<String> roleCodes
    );

    interface UserRoleRow {
        UUID getUserId();
        String getRoleCode();
    }
}
