package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "student_enrollment", schema = "training", uniqueConstraints =
        @UniqueConstraint(name = "uk_enrollment_course_purchase", columnNames = "course_purchase_id"),
        indexes = {
                @Index(name = "idx_enrollment_schedule", columnList = "class_schedule_id"),
                @Index(name = "idx_student_enrollment_student_period",
                        columnList = "student_person_id,start_date,end_date"),
                @Index(name = "idx_enrollment_status_period", columnList = "status,start_date,end_date")
        })
public class StudentEnrollment {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "student_enrollment_id", nullable = false, updatable = false)
    private UUID studentEnrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_person_id", nullable = false)
    private Person studentPerson;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_purchase_id", nullable = false)
    private CoursePurchase coursePurchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentEnrollmentStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
