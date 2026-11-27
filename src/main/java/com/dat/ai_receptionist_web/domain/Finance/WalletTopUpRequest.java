package com.dat.ai_receptionist_web.domain.Finance;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import com.dat.ai_receptionist_web.domain.Security.User;
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

/**
 * Yêu cầu nạp tiền vào ví và thông tin phê duyệt của yêu cầu đó.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallet_top_up_request", schema = "finance")
public class WalletTopUpRequest extends TenantScopedEntity {
    /** Mã định danh duy nhất của yêu cầu nạp tiền. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "top_up_request_id", nullable = false, updatable = false)
    private UUID topUpRequestId;

    /** Mã do phía khách gửi để bảo đảm yêu cầu không được tạo trùng. */
    @Column(name = "client_request_id", nullable = false, updatable = false, unique = true)
    private UUID clientRequestId;

    /** Ví nhận tiền từ yêu cầu nạp. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    /** Người dùng tạo yêu cầu nạp tiền. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false, updatable = false)
    private User requestedByUser;

    /** Người dùng phê duyệt hoặc từ chối yêu cầu nạp tiền. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    /** Giao dịch cộng tiền được tạo sau khi yêu cầu được chấp thuận. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id", unique = true)
    private WalletTransaction walletTransaction;

    /** Phương thức hoặc chế độ thực hiện yêu cầu nạp tiền. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 20)
    private WalletTopUpRequestMode mode;

    /** Trạng thái xử lý hiện tại của yêu cầu nạp tiền. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletTopUpRequestStatus status;

    /** Số tiền yêu cầu nạp vào ví. */
    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    /** Ghi chú của người tạo yêu cầu nạp tiền. */
    @Column(name = "note", length = 500)
    private String note;

    /** Lý do yêu cầu nạp tiền bị từ chối. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Đường dẫn tài liệu hoặc ảnh chứng minh giao dịch nạp tiền. */
    @Column(name = "proof_object_path", length = 500)
    private String proofObjectPath;

    /** Mã băm SHA-256 của tài liệu chứng minh để chống sử dụng trùng. */
    @Column(name = "proof_sha256", length = 64, unique = true)
    private String proofSha256;

    /** Thời điểm yêu cầu được gửi để xử lý. */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Thời điểm yêu cầu được phê duyệt hoặc từ chối. */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

