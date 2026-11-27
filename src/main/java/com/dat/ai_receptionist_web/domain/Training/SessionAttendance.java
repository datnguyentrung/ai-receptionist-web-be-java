package com.dat.ai_receptionist_web.domain.Training;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.enums.Training.AttendanceStatus;
import com.dat.ai_receptionist_web.enums.Training.EvaluationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thông tin điểm danh của học viên hoặc nhân sự trong một buổi học.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "session_attendance", schema = "training", uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_attendance_session_enrollment",
                columnNames = {"class_session_id", "student_enrollment_id"}),
        @UniqueConstraint(name = "uk_session_attendance_session_staff_assignment",
                columnNames = {"class_session_id", "course_staff_assignment_id"}),
        @UniqueConstraint(name = "uk_session_attendance_session_person",
                columnNames = {"class_session_id", "person_id"})
}, indexes = {
        @Index(name = "idx_session_attendance_session", columnList = "class_session_id"),
        @Index(name = "idx_session_attendance_enrollment", columnList = "student_enrollment_id"),
        @Index(name = "idx_session_attendance_course_staff_assignment",
                columnList = "course_staff_assignment_id"),
        @Index(name = "idx_session_attendance_person", columnList = "person_id")
})
public class SessionAttendance extends TenantScopedEntity {
    /** Mã định danh duy nhất của bản ghi điểm danh. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "session_attendance_id", nullable = false, updatable = false)
    private UUID sessionAttendanceId;

    /** Buổi học được điểm danh. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    /** Cá nhân thực tế được ghi nhận điểm danh trong buổi học. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Lượt đăng ký của học viên được điểm danh, nếu bản ghi dành cho học viên. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_enrollment_id")
    private StudentEnrollment studentEnrollment;

    /** Thời điểm học viên hoặc nhân sự check-in buổi học. */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    /** Trạng thái điểm danh của người tham gia. */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    /** Trạng thái đánh giá kết quả học tập trong buổi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", length = 20)
    private EvaluationStatus evaluationStatus;

    /** Phân công nhân sự được điểm danh, nếu bản ghi dành cho nhân sự. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_staff_assignment_id")
    private CourseStaffAssignment courseStaffAssignment;

    /** Ghi chú bổ sung về điểm danh hoặc đánh giá. */
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
