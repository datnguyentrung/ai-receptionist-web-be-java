package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.enums.Catalog.TrainingPlanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {
    Optional<TrainingPlan> findByTrainingPlanIdAndStatus(UUID trainingPlanId, TrainingPlanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TrainingPlan> findWithLockByTrainingPlanIdAndStatus(UUID trainingPlanId, TrainingPlanStatus status);

    List<TrainingPlan> findByStatusOrderByNameAsc(TrainingPlanStatus status);

    @Query("select p from TrainingPlan p where (:status is null or p.status = :status) and (:level is null or p.level = :level)")
    Page<TrainingPlan> findAllFiltered(@Param("status") TrainingPlanStatus status,
                                       @Param("level") com.dat.ai_receptionist_web.enums.Core.ScheduleLevel level,
                                       Pageable pageable);
}
