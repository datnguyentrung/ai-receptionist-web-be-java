package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.enums.Core.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Khung giờ học cố định theo chi nhánh, thứ trong tuần và địa điểm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "class_schedule", schema = "catalog")
public class ClassSchedule {
    /** Mã định danh duy nhất của khung giờ học. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "schedule_id", nullable = false, updatable = false)
    private UUID scheduleId;

    /** Chi nhánh áp dụng khung giờ học. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /** Thứ trong tuần diễn ra lớp học. */
    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", nullable = false, length = 20)
    private Weekday weekday;

    /** Hình thức hoặc địa điểm học của lớp. */
    @Enumerated(EnumType.STRING)
    @Column(name = "location", nullable = false, length = 50)
    private ScheduleLocation location;

    /** Trạng thái hoạt động của khung giờ học. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    /** Giờ bắt đầu lớp học. */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** Giờ kết thúc lớp học. */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @PrePersist
    @PreUpdate
    void validateTimes() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time");
        }
    }
}
