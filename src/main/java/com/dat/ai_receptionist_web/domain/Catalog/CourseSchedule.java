package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Khoảng thời gian hoạt động của khóa học, không đại diện cho một buổi học riêng lẻ. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course_schedule", schema = "catalog", indexes = {
        @Index(name = "idx_course_schedule_course_period", columnList = "course_id,start_date,end_date,status"),
        @Index(name = "idx_course_schedule_class_schedule", columnList = "class_schedule_id")
})
public class CourseSchedule {
    /** Mã định danh duy nhất của lịch khóa học. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "course_schedule_id", nullable = false, updatable = false)
    private UUID courseScheduleId;

    /** Khóa học được áp dụng lịch này. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Khung giờ học cố định được dùng để sinh các buổi học. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_schedule_id", nullable = false)
    private ClassSchedule classSchedule;

    /** Ngày bắt đầu hiệu lực của lịch khóa học. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Ngày kết thúc hiệu lực của lịch khóa học. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Trạng thái hiện tại của lịch khóa học. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    /** Loại buổi học được sinh từ lịch này. */
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    @Builder.Default
    private SessionType sessionType = SessionType.STANDARD;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
