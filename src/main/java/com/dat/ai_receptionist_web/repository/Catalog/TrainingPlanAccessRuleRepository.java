package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAccessRule;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;

import java.util.UUID;
import java.util.List;

public interface TrainingPlanAccessRuleRepository extends JpaRepository<TrainingPlanAccessRule, UUID> {
    boolean existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionType(
            UUID trainingPlanId, UUID courseId, SessionType sessionType);

    @org.springframework.data.jpa.repository.Query("""
        select r from TrainingPlanAccessRule r join fetch r.targetCourse
        where r.trainingPlan.trainingPlanId = :trainingPlanId
    """)
    List<TrainingPlanAccessRule> findDetailedByTrainingPlanId(
            @org.springframework.data.repository.query.Param("trainingPlanId") UUID trainingPlanId);

    @org.springframework.data.jpa.repository.Query(value = "select r from TrainingPlanAccessRule r join fetch r.trainingPlan join fetch r.targetCourse where (:planId is null or r.trainingPlan.trainingPlanId = :planId)", countQuery = "select count(r) from TrainingPlanAccessRule r where (:planId is null or r.trainingPlan.trainingPlanId = :planId)")
    Page<TrainingPlanAccessRule> findAllDetailed(@org.springframework.data.repository.query.Param("planId") UUID planId, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("select r from TrainingPlanAccessRule r join fetch r.trainingPlan join fetch r.targetCourse where r.trainingPlanAccessRuleId = :id")
    java.util.Optional<TrainingPlanAccessRule> findDetailedById(@org.springframework.data.repository.query.Param("id") UUID id);
    @org.springframework.data.jpa.repository.Query("select r from TrainingPlanAccessRule r join fetch r.trainingPlan join fetch r.targetCourse where r.trainingPlan.trainingPlanId in :planIds")
    List<TrainingPlanAccessRule> findDetailedByTrainingPlanIdIn(@org.springframework.data.repository.query.Param("planIds") java.util.Collection<UUID> planIds);
    List<TrainingPlanAccessRule> findByTrainingPlan_TrainingPlanId(UUID planId);
}
