package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Khóa học cung cấp thông tin về cấp độ, sức chứa và trạng thái mở đăng ký.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course", schema = "catalog")
public class Course {
    /** Mã định danh duy nhất của khóa học. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    /** Danh sách lịch hoạt động của khóa học. */
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<CourseSchedule> courseSchedules;

    /**
     * Transitional read helper for legacy service code; API contracts expose courseSchedules only.
     */
    @Transient
    public ClassSchedule getClassSchedule() {
        return courseSchedules == null ? null : courseSchedules.stream().filter(schedule -> schedule.getEndDate() == null).max(Comparator.comparing(CourseSchedule::getStartDate)).map(CourseSchedule::getClassSchedule).orElseGet(() -> courseSchedules.isEmpty() ? null : courseSchedules.get(0).getClassSchedule());
    }

    @Transient
    public ClassSchedule getNextClassSchedule() {
        return null;
    }

    @Transient
    public LocalDate getNextScheduleEffectiveFrom() {
        return null;
    }

    /**
     * @deprecated schedule mutations are now performed through CourseScheduleService.
     */
    @Deprecated
    public void setClassSchedule(ClassSchedule ignored) {
        throw new UnsupportedOperationException("Use COURSE_SCHEDULE");
    }

    /**
     * @deprecated schedule mutations are now performed through CourseScheduleService.
     */
    @Deprecated
    public void setNextClassSchedule(ClassSchedule ignored) {
        throw new UnsupportedOperationException("Use COURSE_SCHEDULE");
    }

    /**
     * @deprecated schedule mutations are now performed through CourseScheduleService.
     */
    @Deprecated
    public void setNextScheduleEffectiveFrom(LocalDate ignored) {
        throw new UnsupportedOperationException("Use COURSE_SCHEDULE");
    }

    /** Tên khóa học. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Cấp độ của khóa học. */
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private ScheduleLevel level;

    /** Số lượng học viên tối đa của một lớp. */
    @Column(name = "capacity", nullable = false)
    private int capacity;

    /** Trạng thái hiện tại của khóa học. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    /** Ngày cuối cùng đã sinh buổi học từ lịch khóa học. */
    @Column(name = "class_session_generated_until")
    private LocalDate classSessionGeneratedUntil;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
