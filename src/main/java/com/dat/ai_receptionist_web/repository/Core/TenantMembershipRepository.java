package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.TenantMembership;
import com.dat.ai_receptionist_web.enums.Core.TenantMembershipStatus;
import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {
    Optional<TenantMembership> findByTenantIdAndUser_UserId(UUID tenantId, UUID userId);

    boolean existsByTenantIdAndUser_UserIdAndStatus(UUID tenantId, UUID userId, TenantMembershipStatus status);

    @Query("""
            select tm
            from TenantMembership tm
            join fetch tm.tenant t
            where tm.user.userId = :userId
              and tm.status = :membershipStatus
              and t.status = :tenantStatus
            order by t.name asc
            """)
    List<TenantMembership> findActiveMembershipsForUser(
            @Param("userId") UUID userId,
            @Param("membershipStatus") TenantMembershipStatus membershipStatus,
            @Param("tenantStatus") TenantStatus tenantStatus
    );

    default List<TenantMembership> findActiveMembershipsForUser(UUID userId) {
        return findActiveMembershipsForUser(userId, TenantMembershipStatus.ACTIVE, TenantStatus.ACTIVE);
    }
}
