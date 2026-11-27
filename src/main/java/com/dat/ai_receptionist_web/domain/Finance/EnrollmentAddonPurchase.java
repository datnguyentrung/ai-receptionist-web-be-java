package com.dat.ai_receptionist_web.domain.Finance;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAddon;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Giao dịch mua gói bổ sung cho một lượt đăng ký khóa học của học viên.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "enrollment_addon_purchase", schema = "finance", uniqueConstraints =
@UniqueConstraint(name = "uk_enrollment_addon_purchase_transaction", columnNames = "wallet_transaction_id"))
public class EnrollmentAddonPurchase extends TenantScopedEntity {
    /** Mã định danh duy nhất của giao dịch mua gói bổ sung. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "enrollment_addon_purchase_id", nullable = false, updatable = false)
    private UUID enrollmentAddonPurchaseId;

    /** Lượt đăng ký khóa học được áp dụng gói bổ sung. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment studentEnrollment;

    /** Gói bổ sung được mua. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_addon_id", nullable = false)
    private TrainingPlanAddon addon;

    /** Giao dịch ví dùng để thanh toán gói bổ sung. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;

    /** Ngày đầu tuần bắt đầu áp dụng gói bổ sung. */
    @Column(name = "effective_week_start", nullable = false)
    private LocalDate effectiveWeekStart;

    /** Ngày cuối cùng gói bổ sung còn hiệu lực. */
    @Column(name = "effective_to", nullable = false)
    private LocalDate effectiveTo;

    /** Số buổi học bổ sung được cộng vào lượt đăng ký. */
    @Column(name = "granted_session_count", nullable = false)
    private int grantedSessionCount;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
