package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.AddonStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gói bổ sung có thể được mua thêm cho một gói đào tạo.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "training_plan_addon", schema = "catalog")
public class TrainingPlanAddon {
    /** Mã định danh duy nhất của gói bổ sung. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "training_plan_addon_id", nullable = false, updatable = false)
    private UUID trainingPlanAddonId;

    /** Gói đào tạo mà gói bổ sung này có thể áp dụng. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    /** Tên của gói bổ sung. */
    @Column(nullable = false, length = 255)
    private String name;

    /** Số buổi học tăng thêm mỗi tuần. */
    @Column(name = "extra_sessions_per_week", nullable = false)
    private int extraSessionsPerWeek;

    /** Giá bán của gói bổ sung. */
    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal price;

    /** Trạng thái phát hành và sử dụng của gói bổ sung. */
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AddonStatus status;
}
