package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.CoursePurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;

public interface CoursePurchaseRepository extends JpaRepository<CoursePurchase, UUID> {
    @Query(value = """
            select purchase
            from CoursePurchase purchase
            join fetch purchase.studentPerson
            join fetch purchase.coursePrice coursePrice
            join fetch coursePrice.course course
            join fetch course.classSchedule classSchedule
            join fetch classSchedule.branch
            left join fetch course.nextClassSchedule nextClassSchedule
            left join fetch nextClassSchedule.branch
            join fetch purchase.debitTransaction
            """,
            countQuery = """
            select count(purchase)
            from CoursePurchase purchase
            """)
    Page<CoursePurchase> findAllDetailed(Pageable pageable);

    Optional<CoursePurchase> findByDebitTransaction_WalletTransactionId(UUID transactionId);
}
