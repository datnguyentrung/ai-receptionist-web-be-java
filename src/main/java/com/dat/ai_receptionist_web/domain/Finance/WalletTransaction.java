package com.dat.ai_receptionist_web.domain.Finance;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Finance.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bút toán phát sinh trên ví, ghi nhận số tiền và số dư trước sau giao dịch.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallet_transaction", schema = "finance", uniqueConstraints =
        @UniqueConstraint(name = "uk_wallet_tx_type_reference", columnNames = {"type", "external_reference"}),
        indexes = @Index(name = "idx_wallet_tx_wallet_created", columnList = "wallet_id,created_at DESC"))
public class WalletTransaction {
    /** Mã định danh duy nhất của giao dịch ví. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "wallet_transaction_id", nullable = false, updatable = false)
    private UUID walletTransactionId;

    /** Ví phát sinh giao dịch. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** Loại nghiệp vụ của giao dịch ví. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private WalletTransactionType type;

    /** Chiều biến động số dư: cộng hoặc trừ. */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private WalletTransactionDirection direction;

    /** Giá trị tiền của giao dịch. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    /** Số dư ví trước khi giao dịch được áp dụng. */
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 0)
    private BigDecimal balanceBefore;

    /** Số dư ví sau khi giao dịch được áp dụng. */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 0)
    private BigDecimal balanceAfter;

    /** Mã tham chiếu bên ngoài dùng để nhận diện và chống trùng giao dịch. */
    @Column(name = "external_reference", nullable = false, length = 150)
    private String externalReference;

    /** Người dùng tạo giao dịch ví. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /** Người dùng kiểm tra hoặc phê duyệt giao dịch ví. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    /** Thời điểm giao dịch được kiểm tra hoặc phê duyệt. */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Ghi chú nghiệp vụ của giao dịch ví. */
    @Column(name = "note", length = 500)
    private String note;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Trạng thái xử lý hiện tại của giao dịch ví. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletTransactionStatus status;
}
