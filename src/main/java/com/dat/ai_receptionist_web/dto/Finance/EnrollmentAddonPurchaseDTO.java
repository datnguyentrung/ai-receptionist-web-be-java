package com.dat.ai_receptionist_web.dto.Finance;
import java.time.*;
import java.util.UUID;
public final class EnrollmentAddonPurchaseDTO {
 private EnrollmentAddonPurchaseDTO() {}
 public record BriefResponse(UUID enrollmentAddonPurchaseId, UUID studentEnrollmentId, UUID trainingPlanAddonId) {}
 public record SimpleResponse(UUID enrollmentAddonPurchaseId, UUID studentEnrollmentId, UUID trainingPlanAddonId, LocalDate effectiveWeekStart, LocalDate effectiveTo, int grantedSessionCount, LocalDateTime createdAt) {}
 public record Response(UUID enrollmentAddonPurchaseId, UUID studentEnrollmentId, UUID trainingPlanAddonId, UUID walletTransactionId, LocalDate effectiveWeekStart, LocalDate effectiveTo, int grantedSessionCount, LocalDateTime createdAt) {}
}
