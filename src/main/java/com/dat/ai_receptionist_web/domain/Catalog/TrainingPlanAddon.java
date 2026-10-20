package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.AddonStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "training_plan_addon", schema = "catalog")
public class TrainingPlanAddon {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "training_plan_addon_id", nullable = false, updatable = false)
    private UUID trainingPlanAddonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "extra_sessions_per_week", nullable = false)
    private int extraSessionsPerWeek;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal price;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AddonStatus status;
}
