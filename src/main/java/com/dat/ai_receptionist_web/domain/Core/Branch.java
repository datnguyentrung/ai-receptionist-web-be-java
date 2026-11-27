package com.dat.ai_receptionist_web.domain.Core;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.enums.Core.BranchStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Chi nhánh hoặc cơ sở hoạt động của trung tâm.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "branch", schema = "core")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Branch extends TenantScopedEntity {

    /** Mã định danh duy nhất của chi nhánh. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    Long branchId;

    /** Tên của chi nhánh. */
    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(max = 100, message = "Tên chi nhánh không quá 100 ký tự")
    @Column(name = "name", nullable = false, length = 100)
    String name;

    /** Địa chỉ của chi nhánh. */
    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255)
    @Column(name = "address", nullable = false)
    String address;

    /** Số điện thoại đường dây nóng của chi nhánh. */
    @Size(max = 20, message = "Hotline không quá 20 ký tự")
    @Column(name = "hotline", length = 20)
    String hotline;

    /** Ngày chi nhánh chính thức khai trương. */
    @NotNull(message = "Ngày khai trương không được để trống")
    @PastOrPresent(message = "Ngày khai trương phải trong quá khứ hoặc hôm nay")
    @Column(name = "opened_date", nullable = false)
    LocalDate openedDate;

    /** Trạng thái hoạt động của chi nhánh. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    BranchStatus status = BranchStatus.OPERATING;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
