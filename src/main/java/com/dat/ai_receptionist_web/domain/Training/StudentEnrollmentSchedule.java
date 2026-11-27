package com.dat.ai_receptionist_web.domain.Training;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;
import java.time.LocalDate;

/**
 * Liên kết một lượt đăng ký học với một lịch khóa học trong khoảng thời gian hiệu lực.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "student_enrollment_schedule", schema = "training", uniqueConstraints =
        @UniqueConstraint(name = "uk_student_enrollment_schedule", columnNames = {"student_enrollment_id", "course_schedule_id", "effective_from"}),
        indexes = @Index(name = "idx_student_enrollment_schedule_course_schedule", columnList = "course_schedule_id"))
public class StudentEnrollmentSchedule extends TenantScopedEntity {
    /** Mã định danh duy nhất của liên kết lịch đăng ký. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "student_enrollment_schedule_id", nullable = false, updatable = false)
    private UUID studentEnrollmentScheduleId;

    /** Lượt đăng ký học sở hữu liên kết lịch. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment studentEnrollment;

    /** Lịch khóa học được gán cho lượt đăng ký. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_schedule_id", nullable = false)
    private CourseSchedule courseSchedule;

    /** Yêu cầu của gói đào tạo mà lịch này đáp ứng. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_requirement_id")
    private TrainingPlanRequirement requirement;

    /** Ngày bắt đầu hiệu lực của lịch đăng ký. */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /** Ngày kết thúc hiệu lực của lịch đăng ký. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** Trạng thái hiện tại của lịch đăng ký. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnrollmentScheduleStatus status = EnrollmentScheduleStatus.ACTIVE;
}
