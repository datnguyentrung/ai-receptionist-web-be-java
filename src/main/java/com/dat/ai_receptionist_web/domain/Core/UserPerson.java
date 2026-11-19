package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Liên kết giữa tài khoản người dùng và hồ sơ cá nhân mà tài khoản đó được phép thao tác.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_person", schema = "core", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_person_relationship",
                columnNames = {"user_id", "person_id", "relationship_type"}),
        indexes = {
                @Index(name = "idx_user_person_person", columnList = "person_id"),
                @Index(name = "idx_user_person_user_relationship_person_active",
                        columnList = "user_id,relationship_type,person_id,active")
        })
public class UserPerson {
    /** Mã định danh duy nhất của liên kết tài khoản và cá nhân. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_person_id", nullable = false, updatable = false)
    private UUID userPersonId;

    /** Tài khoản người dùng trong liên kết. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Hồ sơ cá nhân được liên kết với tài khoản. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Loại quan hệ giữa tài khoản và hồ sơ cá nhân. */
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    private RelationshipType relationshipType;

    /** Cho biết liên kết còn hiệu lực hay không. */
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
