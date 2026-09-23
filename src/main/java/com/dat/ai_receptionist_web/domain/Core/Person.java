package com.dat.ai_receptionist_web.domain.Core;

import com.dat.ai_receptionist_web.enums.Core.Belt;
import com.dat.ai_receptionist_web.enums.Core.PersonStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "person", schema = "core", uniqueConstraints = {
        @UniqueConstraint(name = "uk_person_national_code", columnNames = "national_code"),
        @UniqueConstraint(name = "uk_person_code", columnNames = "person_code")
}, indexes = @Index(name = "idx_person_position", columnList = "position_id"))
public class Person {
    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "gender")
    private Boolean gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_belt", length = 20)
    private Belt currentBelt;

    @Column(name = "email", length = 100)
    @Email
    private String email;

    @Column(name = "national_code", length = 50)
    private String nationalCode;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 512)
    @Column(name = "face_embedding", columnDefinition = "vector(512)")
    private float[] faceEmbedding;

    @Column(name = "face_image_path", length = 500)
    private String faceImagePath;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "person_code", nullable = false, length = 50)
    private String personCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersonStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;
}
