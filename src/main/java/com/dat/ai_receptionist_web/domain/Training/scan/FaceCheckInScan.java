package com.dat.ai_receptionist_web.domain.Training.scan;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Trạng thái của một lượt điểm danh bằng khuôn mặt từ lúc tạo đến khi xử lý xong.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "face_check_in_scan", schema = "training")
public class FaceCheckInScan extends TenantScopedEntity {
    /** Mã yêu cầu duy nhất của lượt điểm danh khuôn mặt. */
    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    /** Mã định danh thiết bị thực hiện quét khuôn mặt. */
    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    /** Mã định danh tài khoản thực hiện hoặc khởi tạo lượt quét. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Mã băm của ticket cấp phép tải ảnh khuôn mặt lên. */
    @Column(name = "upload_ticket_hash", nullable = false, length = 64)
    private String uploadTicketHash;

    /** Thời điểm ticket tải ảnh được sử dụng. */
    @Column(name = "ticket_used_at")
    private Instant ticketUsedAt;

    /** Giai đoạn xử lý hiện tại của lượt điểm danh khuôn mặt. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private FaceCheckInStage stage;

    /** Thời điểm lượt điểm danh khuôn mặt hết hạn. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Thời điểm lượt điểm danh khuôn mặt được tạo. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Thời điểm lượt điểm danh khuôn mặt được cập nhật gần nhất. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Phiên bản dùng để kiểm soát cập nhật đồng thời của lượt điểm danh. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
