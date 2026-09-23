package com.dat.ai_receptionist_web.domain.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.CoursePriceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "course_price", schema = "catalog",
        indexes = @Index(name = "idx_course_price_course", columnList = "course_id"))
public class CoursePrice {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "course_price_id", nullable = false, updatable = false)
    private UUID coursePriceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "duration_months", nullable = false)
    private int durationMonths;

    @Column(name = "session_count", nullable = false)
    private int sessionCount;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 0)
    private BigDecimal basePrice;

    @Column(name = "final_price", nullable = false, precision = 19, scale = 0)
    private BigDecimal finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CoursePriceStatus status;
}
