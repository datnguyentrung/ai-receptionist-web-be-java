package com.dat.ai_receptionist_web.domain.Finance;

import com.dat.ai_receptionist_web.domain.Catalog.CoursePrice;
import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.domain.Core.Person;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "course_purchase", schema = "finance", uniqueConstraints =
        @UniqueConstraint(name = "uk_course_purchase_debit_tx", columnNames = "debit_transaction_id"),
        indexes = @Index(name = "idx_course_purchase_price", columnList = "course_price_id"))
public class CoursePurchase {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "course_purchase_id", nullable = false, updatable = false)
    private UUID coursePurchaseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_person_id", nullable = false)
    private Person studentPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_price_id")
    private CoursePrice coursePrice;

    /** Compatibility link retained while V20-V24 migrate historical purchases. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id")
    private TrainingPlan trainingPlan;

    /** Operational course picked when the plan was purchased. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_course_id")
    private Course homeCourse;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debit_transaction_id", nullable = false)
    private WalletTransaction debitTransaction;
}
