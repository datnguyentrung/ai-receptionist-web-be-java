package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.EnrollmentAddonPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;

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
    @Query(value = "select p from EnrollmentAddonPurchase p join fetch p.studentEnrollment join fetch p.addon join fetch p.walletTransaction where (:enrollmentId is null or p.studentEnrollment.studentEnrollmentId = :enrollmentId) and (:addonId is null or p.addon.trainingPlanAddonId = :addonId)", countQuery = "select count(p) from EnrollmentAddonPurchase p where (:enrollmentId is null or p.studentEnrollment.studentEnrollmentId = :enrollmentId) and (:addonId is null or p.addon.trainingPlanAddonId = :addonId)")
    Page<EnrollmentAddonPurchase> findAllDetailed(@Param("enrollmentId") UUID enrollmentId, @Param("addonId") UUID addonId, Pageable pageable);
    @Query("select p from EnrollmentAddonPurchase p join fetch p.studentEnrollment join fetch p.addon join fetch p.walletTransaction where p.enrollmentAddonPurchaseId = :id")
    java.util.Optional<EnrollmentAddonPurchase> findDetailedById(@Param("id") UUID id);
    boolean existsByAddon_TrainingPlanAddonId(UUID addonId);
}
