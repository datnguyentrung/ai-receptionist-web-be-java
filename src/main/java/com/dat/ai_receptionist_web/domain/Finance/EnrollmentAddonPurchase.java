package com.dat.ai_receptionist_web.domain.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAddon;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "enrollment_addon_purchase", schema = "finance", uniqueConstraints =
        @UniqueConstraint(name = "uk_enrollment_addon_purchase_transaction", columnNames = "wallet_transaction_id"))
public class EnrollmentAddonPurchase {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "enrollment_addon_purchase_id", nullable = false, updatable = false)
    private UUID enrollmentAddonPurchaseId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment studentEnrollment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "training_plan_addon_id", nullable = false)
    private TrainingPlanAddon addon;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "wallet_transaction_id", nullable = false)
    private WalletTransaction walletTransaction;
    @Column(name = "effective_week_start", nullable = false) private LocalDate effectiveWeekStart;
    @Column(name = "effective_to", nullable = false) private LocalDate effectiveTo;
    @Column(name = "granted_session_count", nullable = false) private int grantedSessionCount;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
