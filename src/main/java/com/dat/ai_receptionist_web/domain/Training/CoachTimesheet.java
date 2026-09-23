package com.dat.ai_receptionist_web.domain.Training;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "coach_timesheet", schema = "training", uniqueConstraints =
        @UniqueConstraint(name = "uk_timesheet_assignment_session",
                columnNames = {"course_staff_assignment_id", "class_session_id"}),
        indexes = @Index(name = "idx_coach_timesheet_class_session", columnList = "class_session_id"))
public class CoachTimesheet {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "coach_timesheet_id", nullable = false, updatable = false)
    private UUID coachTimesheetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_staff_assignment_id", nullable = false)
    private CourseStaffAssignment courseStaffAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "note", length = 500)
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
