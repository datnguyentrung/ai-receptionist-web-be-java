package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAddon;
import com.dat.ai_receptionist_web.enums.Catalog.AddonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanAddonRepository extends JpaRepository<TrainingPlanAddon, UUID> {
    Optional<TrainingPlanAddon> findByTrainingPlanAddonIdAndStatus(UUID addonId, AddonStatus status);
}
