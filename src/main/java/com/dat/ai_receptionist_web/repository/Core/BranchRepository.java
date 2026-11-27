package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Page<Branch> findAllByTenantId(UUID tenantId, Pageable pageable);
    Optional<Branch> findByTenantIdAndBranchId(UUID tenantId, Long branchId);
}
