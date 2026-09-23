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
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_person_id", nullable = false, updatable = false)
    private UUID userPersonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 30)
    private RelationshipType relationshipType;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
