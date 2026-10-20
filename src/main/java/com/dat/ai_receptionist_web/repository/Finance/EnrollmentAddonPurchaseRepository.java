package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.EnrollmentAddonPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EnrollmentAddonPurchaseRepository extends JpaRepository<EnrollmentAddonPurchase, UUID> {
    @Query("""
        select p from EnrollmentAddonPurchase p
        join fetch p.addon
        where p.studentEnrollment.studentEnrollmentId = :enrollmentId
          and p.effectiveWeekStart <= :weekEnd
          and p.effectiveTo >= :weekStart
    """)
    List<EnrollmentAddonPurchase> findEffectiveForWeek(
            @Param("enrollmentId") UUID enrollmentId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd);
}
