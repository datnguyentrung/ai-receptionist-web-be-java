package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.enums.Catalog.FulfillmentMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingPlanRequirementRepository extends JpaRepository<TrainingPlanRequirement, UUID> {
    List<TrainingPlanRequirement> findByTrainingPlan_TrainingPlanId(UUID trainingPlanId);
    List<TrainingPlanRequirement> findByTrainingPlan_TrainingPlanIdAndFulfillmentMode(UUID trainingPlanId, FulfillmentMode fulfillmentMode);
}
