package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    @Query(value = """
            select w
            from Wallet w
            join fetch w.person
            """,
            countQuery = """
            select count(w)
            from Wallet w
            """)
    Page<Wallet> findAllDetailed(Pageable pageable);

    Optional<Wallet> findByPerson_PersonId(UUID personId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.person.personId = :personId")
    Optional<Wallet> findByPersonIdForUpdate(@Param("personId") UUID personId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.walletId = :walletId")
    Optional<Wallet> findByIdForUpdate(@Param("walletId") UUID walletId);
}
