package com.dat.ai_receptionist_web.domain.Crm;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.enums.Crm.TrialRegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lịch đăng ký học thử của một khách hàng tiềm năng.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "trial_registration", schema = "crm", indexes = {
        @Index(name = "idx_crm_trial_registration_lead_created", columnList = "lead_id,created_at DESC"),
        @Index(name = "idx_crm_trial_registration_status_created", columnList = "status,created_at DESC"),
        @Index(name = "idx_crm_trial_registration_class_session", columnList = "class_session_id"),
        @Index(name = "idx_crm_trial_registration_coach_person", columnList = "coach_person_id")
})
public class TrialRegistration {
    /**
     * Mã định danh duy nhất của lượt đăng ký học thử.
     */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "trial_registration_id", nullable = false, updatable = false)
    private UUID trialRegistrationId;

    /**
     * Khách hàng tiềm năng đăng ký buổi học thử.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    /**
     * Buổi học cụ thể được chọn cho lượt học thử, nếu có.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id")
    private ClassSession classSession;

    /**
     * Huấn luyện viên phụ trách buổi học thử.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_person_id")
    private Person coachPerson;

    /**
     * Trạng thái hiện tại của lượt đăng ký học thử.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TrialRegistrationStatus status = TrialRegistrationStatus.BOOKED;

    /**
     * Thời điểm khách hàng điểm danh hoặc check-in cho buổi học thử.
     */
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    /**
     * Nhận xét của huấn luyện viên sau buổi học thử.
     */
    @Column(name = "coach_feedback", length = 1000)
    private String coachFeedback;

    /**
     * Ghi chú về kết quả và hướng xử lý sau buổi học thử.
     */
    @Column(name = "outcome_note", length = 1000)
    private String outcomeNote;

    /**
     * Thời điểm bản ghi được tạo.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi được cập nhật gần nhất.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
