package com.dat.ai_receptionist_web.domain.Catalog;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Yêu cầu về số buổi học theo từng loại buổi của một gói đào tạo.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "training_plan_requirement", schema = "catalog", uniqueConstraints =
        @UniqueConstraint(name = "uk_training_plan_requirement", columnNames = {"training_plan_id", "session_type"}))
public class TrainingPlanRequirement extends TenantScopedEntity {
    /** Mã định danh duy nhất của yêu cầu gói đào tạo. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "training_plan_requirement_id", nullable = false, updatable = false)
    private UUID trainingPlanRequirementId;

    /** Gói đào tạo sở hữu yêu cầu này. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    /** Loại buổi học được quy định. */
    @Enumerated(EnumType.STRING) @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    /** Số buổi học bắt buộc mỗi tuần. */
    @Column(name = "sessions_per_week", nullable = false)
    private int sessionsPerWeek;

}
