package com.dat.ai_receptionist_web.domain.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.domain.Core.Person;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Giao dịch mua gói đào tạo của học viên.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "course_purchase", schema = "finance", uniqueConstraints =
        @UniqueConstraint(name = "uk_course_purchase_debit_tx", columnNames = "debit_transaction_id"))
public class CoursePurchase {
    /** Mã định danh duy nhất của giao dịch mua gói đào tạo. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "course_purchase_id", nullable = false, updatable = false)
    private UUID coursePurchaseId;

    /** Học viên thực hiện giao dịch mua. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_person_id", nullable = false)
    private Person studentPerson;

    /** Gói đào tạo được học viên mua. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    /** Giao dịch ghi nợ ví dùng để thanh toán gói đào tạo. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debit_transaction_id", nullable = false)
    private WalletTransaction debitTransaction;
}
