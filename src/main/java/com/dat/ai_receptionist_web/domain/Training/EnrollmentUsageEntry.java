package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.enums.Training.UsageEntryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Immutable
@Table(name = "enrollment_usage_entry", schema = "training")
public class EnrollmentUsageEntry {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "enrollment_usage_entry_id", nullable = false, updatable = false)
    private UUID enrollmentUsageEntryId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment enrollment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_session_booking_id")
    private StudentSessionBooking booking;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;
    @Enumerated(EnumType.STRING) @Column(name = "entry_type", nullable = false, length = 20)
    private UsageEntryType entryType;
    @Column(nullable = false) private int units;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
}
