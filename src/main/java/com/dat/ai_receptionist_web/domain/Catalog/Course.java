package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.CourseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "course", schema = "catalog")
public class Course {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<CourseSchedule> courseSchedules;

    /** Transitional read helper for legacy service code; API contracts expose courseSchedules only. */
    @Transient
    public ClassSchedule getClassSchedule() {
        return courseSchedules == null ? null : courseSchedules.stream()
                .filter(schedule -> schedule.getEndDate() == null)
                .max(Comparator.comparing(CourseSchedule::getStartDate))
                .map(CourseSchedule::getClassSchedule)
                .orElseGet(() -> courseSchedules.isEmpty() ? null : courseSchedules.get(0).getClassSchedule());
    }

    @Transient public ClassSchedule getNextClassSchedule() { return null; }
    @Transient public LocalDate getNextScheduleEffectiveFrom() { return null; }
    /** @deprecated schedule mutations are now performed through CourseScheduleService. */
    @Deprecated public void setClassSchedule(ClassSchedule ignored) { throw new UnsupportedOperationException("Use COURSE_SCHEDULE"); }
    /** @deprecated schedule mutations are now performed through CourseScheduleService. */
    @Deprecated public void setNextClassSchedule(ClassSchedule ignored) { throw new UnsupportedOperationException("Use COURSE_SCHEDULE"); }
    /** @deprecated schedule mutations are now performed through CourseScheduleService. */
    @Deprecated public void setNextScheduleEffectiveFrom(LocalDate ignored) { throw new UnsupportedOperationException("Use COURSE_SCHEDULE"); }

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "class_session_generated_until")
    private LocalDate classSessionGeneratedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
