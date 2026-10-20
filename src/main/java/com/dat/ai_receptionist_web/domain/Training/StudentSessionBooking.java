package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.domain.Finance.EnrollmentAddonPurchase;
import com.dat.ai_receptionist_web.enums.Training.BookingSource;
import com.dat.ai_receptionist_web.enums.Training.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "student_session_booking", schema = "training", uniqueConstraints =
        @UniqueConstraint(name = "uk_student_session_booking", columnNames = {"student_enrollment_id", "class_session_id"}), indexes = {
        @Index(name = "idx_student_session_booking_session_status", columnList = "class_session_id,status"),
        @Index(name = "idx_student_session_booking_enrollment_status", columnList = "student_enrollment_id,status")
})
public class StudentSessionBooking {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "student_session_booking_id", nullable = false, updatable = false)
    private UUID studentSessionBookingId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment enrollment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "training_plan_requirement_id")
    private TrainingPlanRequirement requirement;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "enrollment_addon_purchase_id")
    private EnrollmentAddonPurchase addonPurchase;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingSource source;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingStatus status;
    @Column(name = "booked_at", nullable = false) private LocalDateTime bookedAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @Column(name = "settled_at") private LocalDateTime settledAt;
}
