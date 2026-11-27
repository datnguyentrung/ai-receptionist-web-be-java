package com.dat.ai_receptionist_web.domain.Core;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chức vụ hoặc vị trí công việc được gán cho cá nhân.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "position", schema = "core", uniqueConstraints =
        @UniqueConstraint(name = "uk_position_code", columnNames = "code"))
public class Position extends TenantScopedEntity {
    /** Mã định danh duy nhất của chức vụ. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "position_id", nullable = false, updatable = false)
    private UUID positionId;

    /** Mã nghiệp vụ duy nhất của chức vụ. */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** Tên của chức vụ. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Mô tả nhiệm vụ và phạm vi của chức vụ. */
    @Column(name = "description", length = 500)
    private String description;

    /** Cho biết chức vụ còn đang được sử dụng hay không. */
    @Column(name = "active", nullable = false)
    private boolean active;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
