package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TrainingPlanRequirementRepository extends JpaRepository<TrainingPlanRequirement, UUID> {
    List<TrainingPlanRequirement> findByTrainingPlan_TrainingPlanId(UUID trainingPlanId);
    @Query(value = "select r from TrainingPlanRequirement r join fetch r.trainingPlan where (:planId is null or r.trainingPlan.trainingPlanId = :planId)", countQuery = "select count(r) from TrainingPlanRequirement r where (:planId is null or r.trainingPlan.trainingPlanId = :planId)")
    Page<TrainingPlanRequirement> findAllDetailed(@Param("planId") UUID planId, Pageable pageable);
    @Query("select r from TrainingPlanRequirement r join fetch r.trainingPlan where r.trainingPlanRequirementId = :id")
    java.util.Optional<TrainingPlanRequirement> findDetailedById(@Param("id") UUID id);
    @Query("select r from TrainingPlanRequirement r join fetch r.trainingPlan where r.trainingPlan.trainingPlanId in :planIds")
    List<TrainingPlanRequirement> findDetailedByTrainingPlanIdIn(@Param("planIds") java.util.Collection<UUID> planIds);
}
