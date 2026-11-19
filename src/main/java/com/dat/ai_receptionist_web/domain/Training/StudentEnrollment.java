package com.dat.ai_receptionist_web.domain.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.enums.Training.StudentEnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lượt đăng ký học của học viên sau khi mua một gói đào tạo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "student_enrollment", schema = "training", uniqueConstraints =
        @UniqueConstraint(name = "uk_enrollment_course_purchase", columnNames = "course_purchase_id"),
        indexes = {
                @Index(name = "idx_student_enrollment_student_period",
                        columnList = "student_person_id,start_date,end_date"),
                @Index(name = "idx_enrollment_status_period", columnList = "status,start_date,end_date")
        })
public class StudentEnrollment {
    /** Mã định danh duy nhất của lượt đăng ký học. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "student_enrollment_id", nullable = false, updatable = false)
    private UUID studentEnrollmentId;

    /** Học viên sở hữu lượt đăng ký. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_person_id", nullable = false)
    private Person studentPerson;

    /** Giao dịch mua gói đào tạo làm phát sinh lượt đăng ký. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_purchase_id", nullable = false)
    private CoursePurchase coursePurchase;

    /** Danh sách lịch khóa học được gán cho lượt đăng ký. */
    @OneToMany(mappedBy = "studentEnrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentEnrollmentSchedule> schedules = new LinkedHashSet<>();

    /**
     * Thay thế toàn bộ lịch hiện tại bằng danh sách lịch được chọn.
     */
    public void replaceSchedules(Set<StudentEnrollmentSchedule> selected) {
        schedules.clear();
        selected.forEach(link -> {
            link.setStudentEnrollment(this);
            schedules.add(link);
        });
    }

    /** Ngày bắt đầu hiệu lực của lượt đăng ký. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Ngày kết thúc hiệu lực của lượt đăng ký. */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Trạng thái hiện tại của lượt đăng ký. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentEnrollmentStatus status;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
