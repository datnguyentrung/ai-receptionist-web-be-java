package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
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
@Table(name = "course", schema = "catalog", indexes = {
        @Index(name = "idx_course_next_schedule_effective", columnList = "next_schedule_effective_from"),
        @Index(name = "idx_course_schedule_status", columnList = "schedule_id,status"),
        @Index(name = "idx_course_next_schedule", columnList = "next_schedule_id")
})
public class Course {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_schedule_id")
    private ClassSchedule nextClassSchedule;

    @Column(name = "next_schedule_effective_from")
    private LocalDate nextScheduleEffectiveFrom;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "class_session_generated_until")
    private LocalDate classSessionGeneratedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
