package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.enums.Catalog.TrainingPlanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {
    Optional<TrainingPlan> findByTrainingPlanIdAndStatus(UUID trainingPlanId, TrainingPlanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TrainingPlan> findWithLockByTrainingPlanIdAndStatus(UUID trainingPlanId, TrainingPlanStatus status);
}
