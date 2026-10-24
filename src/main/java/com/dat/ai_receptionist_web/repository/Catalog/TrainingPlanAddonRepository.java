package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAddon;
import com.dat.ai_receptionist_web.enums.Catalog.AddonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TrainingPlanAddonRepository extends JpaRepository<TrainingPlanAddon, UUID> {
    Optional<TrainingPlanAddon> findByTrainingPlanAddonIdAndStatus(UUID addonId, AddonStatus status);
    @Query(value = "select a from TrainingPlanAddon a join fetch a.trainingPlan where (:planId is null or a.trainingPlan.trainingPlanId = :planId) and (:status is null or a.status = :status)", countQuery = "select count(a) from TrainingPlanAddon a where (:planId is null or a.trainingPlan.trainingPlanId = :planId) and (:status is null or a.status = :status)")
    Page<TrainingPlanAddon> findAllDetailed(@Param("planId") UUID planId, @Param("status") AddonStatus status, Pageable pageable);
    @Query("select a from TrainingPlanAddon a join fetch a.trainingPlan where a.trainingPlanAddonId = :id")
    Optional<TrainingPlanAddon> findDetailedById(@Param("id") UUID id);
    List<TrainingPlanAddon> findByTrainingPlan_TrainingPlanId(UUID planId);
}
