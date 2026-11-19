package com.dat.ai_receptionist_web.domain.Crm;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Crm.LeadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Khách hàng tiềm năng chưa trở thành học viên chính thức.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "lead", schema = "crm", indexes = {
        @Index(name = "idx_crm_lead_phone", columnList = "phone_number"),
        @Index(name = "idx_crm_lead_status_follow_up", columnList = "status,next_follow_up_at"),
        @Index(name = "idx_crm_lead_assigned_status_follow_up",
                columnList = "assigned_user_id,status,next_follow_up_at"),
        @Index(name = "idx_crm_lead_converted_person", columnList = "converted_person_id")
})
public class Lead {
    /**
     * Mã định danh duy nhất của khách hàng tiềm năng.
     */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "lead_id", nullable = false, updatable = false)
    private UUID leadId;

    /**
     * Họ và tên đầy đủ của khách hàng tiềm năng.
     */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Số điện thoại chính dùng để liên hệ với khách hàng.
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Địa chỉ email dùng để liên hệ với khách hàng.
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * Ngày sinh của khách hàng tiềm năng.
     */
    @Column(name = "birth_date", nullable = false, length = 10)
    private LocalDate birthDate;

    /**
     * Trạng thái hiện tại của khách hàng trong quy trình tư vấn.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    /**
     * Nhân viên đang phụ trách tư vấn và chăm sóc khách hàng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    /**
     * Thời điểm dự kiến liên hệ hoặc chăm sóc khách hàng tiếp theo.
     */
    @Column(name = "next_follow_up_at")
    private LocalDateTime nextFollowUpAt;

    /**
     * Thời điểm khách hàng được liên hệ gần nhất.
     */
    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    /**
     * Ghi chú nội bộ về quá trình tư vấn và chăm sóc khách hàng.
     */
    @Column(name = "note", length = 1000)
    private String note;

    /**
     * Hồ sơ học viên được tạo khi khách hàng chuyển đổi thành công.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_person_id")
    private Person convertedPerson;

    /**
     * Thời điểm khách hàng được chuyển đổi thành học viên chính thức.
     */
    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

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
