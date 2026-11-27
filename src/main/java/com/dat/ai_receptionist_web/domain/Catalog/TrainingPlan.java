package com.dat.ai_receptionist_web.domain.Catalog;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.enums.Catalog.TrainingPlanStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gói đào tạo xác định thời lượng, số buổi và giá bán cho học viên.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "training_plan", schema = "catalog")
public class TrainingPlan extends TenantScopedEntity {
    /** Mã định danh duy nhất của gói đào tạo. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "training_plan_id", nullable = false, updatable = false)
    private UUID trainingPlanId;

    /** Tên của gói đào tạo. */
    @Column(nullable = false, length = 255)
    private String name;

    /** Cấp độ áp dụng của gói đào tạo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleLevel level;

    /** Thời lượng của gói đào tạo, tính theo tháng. */
    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    /** Số buổi học cơ bản được bao gồm trong gói. */
    @Column(name = "included_session_count", nullable = false)
    private int includedSessionCount;

    /** Giá niêm yết ban đầu của gói đào tạo. */
    @Column(name = "base_price", nullable = false, precision = 19, scale = 0)
    private BigDecimal basePrice;

    /** Giá bán thực tế của gói đào tạo. */
    @Column(name = "final_price", nullable = false, precision = 19, scale = 0)
    private BigDecimal finalPrice;

    /** Trạng thái phát hành và sử dụng của gói đào tạo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingPlanStatus status;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
