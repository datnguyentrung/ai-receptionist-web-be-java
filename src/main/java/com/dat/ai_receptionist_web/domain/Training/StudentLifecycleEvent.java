package com.dat.ai_receptionist_web.domain.Training;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Training.EventStatus;
import com.dat.ai_receptionist_web.enums.Training.EventType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(
        name = "student_lifecycle_event",
        schema = "training",
        indexes = {
                @Index(
                        name = "idx_student_lifecycle_event_student",
                        columnList = "student_person_id"
                ),
                @Index(
                        name = "idx_student_lifecycle_event_type_date",
                        columnList = "event_type,event_date"
                )
        }
)
public class StudentLifecycleEvent extends TenantScopedEntity {

    @Id
    @UuidGenerator
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId; // ID sự kiện

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_person_id", nullable = false, updatable = false)
    private Person studentPersonId; // Học viên phát sinh sự kiện

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30, updatable = false)
    private EventType eventType; // LEAVE hoặc REACTIVATED

    @Column(name = "event_date", nullable = false, updatable = false)
    private LocalDate eventDate; // Ngày thực sự nghỉ hoặc quay lại

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 20)
    private EventStatus eventStatus = EventStatus.VALID; // VALID hoặc VOIDED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy; // Người tạo sự kiện

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Thời điểm tạo record

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by")
    private User voidedBy; // Người hủy sự kiện

    @Column(name = "voided_at")
    private LocalDateTime voidedAt; // Thời điểm hủy sự kiện

    @Column(name = "void_reason")
    private String voidReason; // Lý do hủy do thao tác nhầm

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (eventStatus == null) {
            eventStatus = EventStatus.VALID;
        }
    }

    /**
     * Hủy một sự kiện được tạo nhầm.
     * Record vẫn được giữ lại để audit nhưng không còn hiệu lực nghiệp vụ.
     */
    public void voidEvent(User voidedBy, String reason) {
        this.eventStatus = EventStatus.VOIDED;
        this.voidedBy = voidedBy;
        this.voidReason = reason;
        this.voidedAt = LocalDateTime.now();
    }
}
