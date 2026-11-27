package com.dat.ai_receptionist_web.repository.Core;

import com.dat.ai_receptionist_web.domain.Core.Tenant;
import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByCode(String code);
    boolean existsByCode(String code);
    List<Tenant> findAllByStatusOrderByNameAsc(TenantStatus status);
}
