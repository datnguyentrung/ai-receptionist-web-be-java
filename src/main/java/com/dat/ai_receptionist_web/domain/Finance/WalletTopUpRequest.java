package com.dat.ai_receptionist_web.domain.Finance;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestMode;
import com.dat.ai_receptionist_web.enums.Finance.WalletTopUpRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallet_top_up_request", schema = "finance")
public class WalletTopUpRequest {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "top_up_request_id", nullable = false, updatable = false)
    private UUID topUpRequestId;

    @Column(name = "client_request_id", nullable = false, updatable = false, unique = true)
    private UUID clientRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false, updatable = false)
    private User requestedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", unique = true)
    private WalletTransaction walletTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 20)
    private WalletTopUpRequestMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletTopUpRequestStatus status;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "proof_object_path", length = 500)
    private String proofObjectPath;

    @Column(name = "proof_sha256", length = 64, unique = true)
    private String proofSha256;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

