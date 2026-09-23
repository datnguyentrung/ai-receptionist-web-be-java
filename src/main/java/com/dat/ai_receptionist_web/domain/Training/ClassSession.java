package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.enums.Training.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "class_session", schema = "training", indexes = {
        @Index(name = "idx_class_session_date_time", columnList = "session_date,start_time"),
        @Index(name = "idx_class_session_lifecycle", columnList = "status,session_date,start_time"),
        @Index(name = "idx_class_session_closure", columnList = "status,is_attendance_closed,session_date"),
        @Index(name = "idx_class_session_course_session_date", columnList = "course_id,session_date")
})
public class ClassSession {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "class_session_id", nullable = false, updatable = false)
    private UUID classSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "is_attendance_closed", nullable = false)
    private boolean attendanceClosed;

    @Column(name = "attendance_reopened_until")
    private LocalDateTime attendanceReopenedUntil;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "note", length = 500)
    private String note;
}
