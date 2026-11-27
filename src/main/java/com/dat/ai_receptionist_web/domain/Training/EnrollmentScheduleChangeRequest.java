package com.dat.ai_receptionist_web.domain.Training;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleChangeRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Yêu cầu thay đổi lịch học hiện tại của một lượt đăng ký khóa học.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "enrollment_schedule_change_request", schema = "training")
public class EnrollmentScheduleChangeRequest extends TenantScopedEntity {
    /** Mã định danh duy nhất của yêu cầu đổi lịch. */
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "enrollment_schedule_change_request_id", nullable = false, updatable = false)
    private UUID enrollmentScheduleChangeRequestId;

    /** Lịch học hiện tại cần được thay đổi. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_schedule_id", nullable = false)
    private StudentEnrollmentSchedule currentSchedule;

    /** Lịch khóa học mục tiêu mà học viên muốn chuyển sang. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_course_schedule_id", nullable = false)
    private CourseSchedule targetCourseSchedule;

    /** Ngày bắt đầu áp dụng lịch khóa học mới. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Ghi chú hoặc lý do của yêu cầu đổi lịch. */
    @Column(length = 1000)
    private String note;

    /** Trạng thái xử lý hiện tại của yêu cầu đổi lịch. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentScheduleChangeRequestStatus status;

    /** Người dùng tạo yêu cầu đổi lịch. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    /** Người dùng phê duyệt hoặc từ chối yêu cầu đổi lịch. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    /** Thời điểm yêu cầu được phê duyệt hoặc từ chối. */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Ghi chú của người duyệt yêu cầu đổi lịch. */
    @Column(name = "review_note", length = 1000)
    private String reviewNote;
}
