package com.dat.ai_receptionist_web.domain.Training;

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
                columnNames = {"class_session_id", "course_staff_assignment_id"})
})
public class SessionAttendance {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "session_attendance_id", nullable = false, updatable = false)
    private UUID sessionAttendanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_enrollment_id")
    private StudentEnrollment studentEnrollment;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", length = 20)
    private EvaluationStatus evaluationStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_staff_assignment_id")
    private CourseStaffAssignment courseStaffAssignment;

    @Column(name = "note", length = 500)
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
