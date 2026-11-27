package com.dat.ai_receptionist_web.domain.Training;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.domain.Finance.EnrollmentAddonPurchase;
import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Đăng ký của học viên cho một buổi học cụ thể.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_session_booking", schema = "training", uniqueConstraints =
@UniqueConstraint(name = "uk_student_session_booking", columnNames = {"student_enrollment_id", "class_session_id"}), indexes = {
        @Index(name = "idx_student_session_booking_session_status", columnList = "class_session_id,status"),
        @Index(name = "idx_student_session_booking_enrollment_status", columnList = "student_enrollment_id,status")
})
public class StudentSessionBooking extends TenantScopedEntity {
    /** Mã định danh duy nhất của lượt đăng ký buổi học. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "student_session_booking_id", nullable = false, updatable = false)
    private UUID studentSessionBookingId;
    /** Lượt đăng ký khóa học của học viên. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment enrollment;
    /** Buổi học được học viên đăng ký. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;
    /** Yêu cầu của gói đào tạo mà lượt đăng ký này đáp ứng. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_requirement_id")
    private TrainingPlanRequirement requirement;
    /** Giao dịch mua gói bổ sung cung cấp buổi học, nếu có. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_addon_purchase_id")
    private EnrollmentAddonPurchase addonPurchase;
    /** Yêu cầu nghỉ làm phát sinh buổi học bù, nếu có. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;
    /** Nguồn tạo lượt đăng ký buổi học. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingSource source;
    /** Trạng thái hiện tại của lượt đăng ký buổi học. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;
    /** Thời điểm học viên đăng ký buổi học. */
    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;
    /** Thời điểm lượt đăng ký buổi học bị hủy. */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    /** Thời điểm lượt đăng ký buổi học được quyết toán. */
    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}
