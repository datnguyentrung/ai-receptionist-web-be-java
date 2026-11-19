package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Một buổi học cụ thể được sinh từ lịch khóa học để điểm danh và quản lý vận hành.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "class_session", schema = "training", indexes = {
        @Index(name = "idx_class_session_date_time", columnList = "session_date,start_time"),
        @Index(name = "idx_class_session_lifecycle", columnList = "status,session_date,start_time"),
        @Index(name = "idx_class_session_closure", columnList = "status,is_attendance_closed,session_date"),
        @Index(name = "idx_class_session_course_session_date", columnList = "course_id,session_date"),
        @Index(name = "idx_class_session_course_status", columnList = "course_id,status"),
        @Index(name = "idx_class_session_schedule_date", columnList = "course_schedule_id,session_date")
})
public class ClassSession {
    /** Mã định danh duy nhất của buổi học. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "class_session_id", nullable = false, updatable = false)
    private UUID classSessionId;

    /** Khóa học diễn ra trong buổi học. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Lịch khóa học làm cơ sở sinh buổi học này. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_schedule_id", nullable = false)
    private CourseSchedule courseSchedule;

    /** Bản chụp thông tin thời gian và địa điểm tại thời điểm sinh buổi học. */
    @Embedded
    private ClassSessionScheduleSnapshot scheduleSnapshot;

    /** Ngày diễn ra buổi học. */
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    /** Trạng thái hiện tại của buổi học. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    /** Cho biết việc điểm danh của buổi học đã đóng hay chưa. */
    @Column(name = "is_attendance_closed", nullable = false)
    private boolean attendanceClosed;

    /** Thời điểm mở lại điểm danh, nếu buổi học được phép mở lại. */
    @Column(name = "attendance_reopened_until")
    private LocalDateTime attendanceReopenedUntil;

    /** Giờ bắt đầu thực tế của buổi học. */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Giờ kết thúc thực tế của buổi học. */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Ghi chú vận hành của buổi học. */
    @Column(name = "note", length = 500)
    private String note;
}
