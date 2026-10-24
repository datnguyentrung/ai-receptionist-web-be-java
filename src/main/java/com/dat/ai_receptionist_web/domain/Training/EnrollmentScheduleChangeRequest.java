package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Training.EnrollmentScheduleChangeRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "enrollment_schedule_change_request", schema = "training")
public class EnrollmentScheduleChangeRequest {
    @Id @GeneratedValue @UuidGenerator
    @Column(name = "enrollment_schedule_change_request_id", nullable = false, updatable = false)
    private UUID enrollmentScheduleChangeRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_schedule_id", nullable = false)
    private StudentEnrollmentSchedule currentSchedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_course_schedule_id", nullable = false)
    private CourseSchedule targetCourseSchedule;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentScheduleChangeRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;
}
