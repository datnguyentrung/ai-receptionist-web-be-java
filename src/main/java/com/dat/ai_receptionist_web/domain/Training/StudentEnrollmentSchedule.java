package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "student_enrollment_schedule", schema = "training", uniqueConstraints =
        @UniqueConstraint(name = "uk_student_enrollment_schedule", columnNames = {"student_enrollment_id", "course_schedule_id"}),
        indexes = @Index(name = "idx_student_enrollment_schedule_course_schedule", columnList = "course_schedule_id"))
public class StudentEnrollmentSchedule {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "student_enrollment_schedule_id", nullable = false, updatable = false)
    private UUID studentEnrollmentScheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment studentEnrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_schedule_id", nullable = false)
    private CourseSchedule courseSchedule;
}
