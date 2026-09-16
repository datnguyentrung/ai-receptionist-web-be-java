package com.dat.ai_receptionist_web.mapper.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import com.dat.ai_receptionist_web.domain.Finance.WalletTransaction;
import com.dat.ai_receptionist_web.domain.Training.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Finance.WalletCommandDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletCommandMapper {
    default WalletCommandDTO.TransactionResponse toTransactionResponse(
            WalletTransaction transaction,
            CoursePurchase purchase,
            StudentEnrollment enrollment
    ) {
        if (transaction == null) {
            return null;
        }
        return new WalletCommandDTO.TransactionResponse(
                transaction.getWalletTransactionId(),
                transaction.getWallet() == null ? null : transaction.getWallet().getWalletId(),
                transaction.getType(),
                transaction.getDirection(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getExternalReference(),
                purchase == null ? null : purchase.getCoursePurchaseId(),
                enrollment == null ? null : enrollment.getStudentEnrollmentId(),
                transaction.getReviewedAt()
        );
    }
}
