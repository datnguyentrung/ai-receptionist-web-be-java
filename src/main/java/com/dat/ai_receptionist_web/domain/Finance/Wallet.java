package com.dat.ai_receptionist_web.domain.Finance;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.enums.Finance.WalletStatus;
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
 * Ví điện tử dùng để thanh toán và ghi nhận số dư của một cá nhân.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallet", schema = "finance", uniqueConstraints =
        @UniqueConstraint(name = "uk_wallet_person", columnNames = "person_id"))
public class Wallet {
    /** Mã định danh duy nhất của ví. */
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    /** Cá nhân sở hữu ví. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Số dư hiện tại của ví. */
    @Column(name = "balance", nullable = false, precision = 19, scale = 0)
    private BigDecimal balance;

    /** Trạng thái hoạt động của ví. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
