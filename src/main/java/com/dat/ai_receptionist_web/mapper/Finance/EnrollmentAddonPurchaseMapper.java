package com.dat.ai_receptionist_web.mapper.Finance;
import com.dat.ai_receptionist_web.domain.Finance.EnrollmentAddonPurchase;
import com.dat.ai_receptionist_web.dto.Finance.EnrollmentAddonPurchaseDTO;
import org.mapstruct.*;
@Mapper(componentModel="spring")
public interface EnrollmentAddonPurchaseMapper {
 @Mapping(target="studentEnrollmentId",source="studentEnrollment.studentEnrollmentId") @Mapping(target="trainingPlanAddonId",source="addon.trainingPlanAddonId") EnrollmentAddonPurchaseDTO.BriefResponse toBriefResponse(EnrollmentAddonPurchase entity);
 @Mapping(target="studentEnrollmentId",source="studentEnrollment.studentEnrollmentId") @Mapping(target="trainingPlanAddonId",source="addon.trainingPlanAddonId") EnrollmentAddonPurchaseDTO.SimpleResponse toSimpleResponse(EnrollmentAddonPurchase entity);
 @Mapping(target="studentEnrollmentId",source="studentEnrollment.studentEnrollmentId") @Mapping(target="trainingPlanAddonId",source="addon.trainingPlanAddonId") @Mapping(target="walletTransactionId",source="walletTransaction.walletTransactionId") EnrollmentAddonPurchaseDTO.Response toResponse(EnrollmentAddonPurchase entity);
}
