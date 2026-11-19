package com.dat.ai_receptionist_web.repository.Crm;

import com.dat.ai_receptionist_web.domain.Crm.TrialRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrialRegistrationRepository extends JpaRepository<TrialRegistration, UUID> {
    @Override
    @EntityGraph(attributePaths = {"lead", "coachPerson"})
    Page<TrialRegistration> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"lead", "coachPerson"})
    Optional<TrialRegistration> findDetailedByTrialRegistrationId(UUID trialRegistrationId);

    boolean existsByLead_LeadId(UUID leadId);
}
