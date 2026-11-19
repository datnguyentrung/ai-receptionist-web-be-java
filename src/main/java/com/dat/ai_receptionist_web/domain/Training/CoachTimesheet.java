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

/**
 * Chấm công của huấn luyện viên cho từng buổi học được phân công.
 */
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
    /** Mã định danh duy nhất của bản ghi chấm công. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "coach_timesheet_id", nullable = false, updatable = false)
    private UUID coachTimesheetId;

    /** Phân công huấn luyện viên tương ứng với buổi học. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_staff_assignment_id", nullable = false)
    private CourseStaffAssignment courseStaffAssignment;

    /** Buổi học được chấm công. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    /** Giờ huấn luyện viên check-in. */
    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    /** Giờ huấn luyện viên check-out. */
    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    /** Ghi chú về việc chấm công hoặc điều chỉnh giờ làm. */
    @Column(name = "note", length = 500)
    private String note;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
