package com.dat.ai_receptionist_web.repository.Finance;

import com.dat.ai_receptionist_web.domain.Finance.WalletTopUpRequest;
import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletTopUpRequestRepository extends JpaRepository<WalletTopUpRequest, UUID> {
    /**
     * Serializes creation attempts for the same client-generated UUID, including
     * the interval before a row exists and can be locked normally.
     */
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(cast(:clientRequestId as text), 0))", nativeQuery = true)
    Object lockClientRequestId(@Param("clientRequestId") UUID clientRequestId);

    @EntityGraph(attributePaths = {"wallet", "wallet.person", "requestedByUser", "reviewedByUser", "walletTransaction"})
    Optional<WalletTopUpRequest> findByClientRequestId(UUID clientRequestId);

    @EntityGraph(attributePaths = {"wallet", "wallet.person", "requestedByUser", "reviewedByUser", "walletTransaction"})
    @Query("select request from WalletTopUpRequest request where request.topUpRequestId = :id")
    Optional<WalletTopUpRequest> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"wallet", "wallet.person", "requestedByUser", "reviewedByUser", "walletTransaction"})
    @Query("select request from WalletTopUpRequest request where request.topUpRequestId = :id")
    Optional<WalletTopUpRequest> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByProofSha256AndTopUpRequestIdNot(String proofSha256, UUID topUpRequestId);

    @EntityGraph(attributePaths = {"wallet", "wallet.person", "requestedByUser", "reviewedByUser", "walletTransaction"})
    Page<WalletTopUpRequest> findByRequestedByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"wallet", "wallet.person", "requestedByUser", "reviewedByUser", "walletTransaction"})
    Page<WalletTopUpRequest> findByStatusOrderByCreatedAtAsc(WalletTopUpRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"wallet", "wallet.person"})
    java.util.List<WalletTopUpRequest> findByStatusAndCreatedAtBefore(
            WalletTopUpRequestStatus status, java.time.LocalDateTime cutoff);
}

