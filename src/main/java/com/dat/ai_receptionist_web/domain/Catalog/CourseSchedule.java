package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** A dated operating timetable of a course, never an individual lesson. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course_schedule", schema = "catalog", indexes = {
        @Index(name = "idx_course_schedule_course_period", columnList = "course_id,start_date,end_date,status"),
        @Index(name = "idx_course_schedule_class_schedule", columnList = "class_schedule_id")
})
public class CourseSchedule {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "course_schedule_id", nullable = false, updatable = false)
    private UUID courseScheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
