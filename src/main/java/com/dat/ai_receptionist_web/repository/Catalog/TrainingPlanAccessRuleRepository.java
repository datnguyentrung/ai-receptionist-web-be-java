package com.dat.ai_receptionist_web.repository.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAccessRule;
import com.dat.ai_receptionist_web.enums.Catalog.PlanAccessMode;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrainingPlanAccessRuleRepository extends JpaRepository<TrainingPlanAccessRule, UUID> {
    boolean existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndAccessMode(
            UUID trainingPlanId, UUID courseId, PlanAccessMode accessMode);
    boolean existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndSessionTypeAndAccessMode(
            UUID trainingPlanId, UUID courseId, SessionType sessionType, PlanAccessMode accessMode);
}
