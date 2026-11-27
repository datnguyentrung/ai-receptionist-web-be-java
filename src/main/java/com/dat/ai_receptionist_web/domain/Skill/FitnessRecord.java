package com.dat.ai_receptionist_web.domain.Skill;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Core.Person;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Kết quả ghi nhận một lần thực hiện bài tập thể lực của học viên.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "fitness_record", schema = "skill")
public class FitnessRecord extends TenantScopedEntity {
    /** Mã định danh duy nhất của bản ghi thể lực. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fitness_record_id")
    private Long fitnessRecordId;

    /** Ngày ghi nhận kết quả luyện tập. */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /** Học viên thực hiện bài tập. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_person_id", nullable = false)
    private Person student;

    /** Thời lượng thực tế của lần luyện tập. */
    @Column(name = "duration", nullable = false)
    private int duration;

    /** Bài tập thể lực được thực hiện. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fitness_id", nullable = false)
    private Fitness fitness;

    /** Huấn luyện viên ghi nhận kết quả luyện tập. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_coach_id", nullable = false)
    private Person recordedByCoach;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
