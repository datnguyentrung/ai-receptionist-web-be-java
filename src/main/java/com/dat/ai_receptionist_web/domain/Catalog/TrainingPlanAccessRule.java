package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Quy tắc cho phép gói đào tạo sử dụng một khóa học theo từng loại buổi.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "training_plan_access_rule", schema = "catalog", uniqueConstraints =
        @UniqueConstraint(name = "uk_training_plan_access_rule", columnNames = {"training_plan_id", "target_course_id", "session_type"}))
public class TrainingPlanAccessRule {
    /** Mã định danh duy nhất của quy tắc truy cập. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "training_plan_access_rule_id", nullable = false, updatable = false)
    private UUID trainingPlanAccessRuleId;

    /** Gói đào tạo áp dụng quy tắc. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    /** Khóa học được phép sử dụng trong gói. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_course_id", nullable = false)
    private Course targetCourse;

    /** Loại buổi học được hưởng quyền truy cập khóa học. */
    @Enumerated(EnumType.STRING) @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

}
