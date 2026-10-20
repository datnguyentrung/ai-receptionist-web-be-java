package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.PlanAccessMode;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "training_plan_access_rule", schema = "catalog", uniqueConstraints =
        @UniqueConstraint(name = "uk_training_plan_access_rule", columnNames = {"training_plan_id", "target_course_id", "session_type", "access_mode"}))
public class TrainingPlanAccessRule {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "training_plan_access_rule_id", nullable = false, updatable = false)
    private UUID trainingPlanAccessRuleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_course_id", nullable = false)
    private Course targetCourse;

    @Enumerated(EnumType.STRING) @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING) @Column(name = "access_mode", nullable = false, length = 20)
    private PlanAccessMode accessMode;
}
