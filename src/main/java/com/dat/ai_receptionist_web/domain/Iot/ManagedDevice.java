package com.dat.ai_receptionist_web.domain.Iot;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thiết bị IoT được quản lý, cấu hình và theo dõi vòng đời trong hệ thống.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @EntityListeners(AuditingEntityListener.class)
@Table(name = "managed_device", schema = "iot")
public class ManagedDevice extends TenantScopedEntity {
    /** Mã định danh duy nhất của thiết bị. */
    @Id @GeneratedValue @UuidGenerator @Column(name="device_id")
    private UUID deviceId;

    /** Mã nghiệp vụ duy nhất của thiết bị. */
    @Column(name="device_code", nullable=false, unique=true)
    private String deviceCode;

    /** Tên hiển thị của thiết bị. */
    @Column(name="display_name", nullable=false)
    private String displayName;

    /** Loại thiết bị IoT. */
    @Column(name="device_type", nullable=false)
    private String deviceType;

    /** Trạng thái hiện tại trong vòng đời quản lý thiết bị. */
    @Column(name="lifecycle_status", nullable=false)
    private String lifecycleStatus;

    /** Mã định danh chi nhánh được gán cho thiết bị. */
    private Long branchId;

    /** Mã định danh thiết bị gateway sương mù quản lý thiết bị này. */
    private UUID fogGatewayId;

    /** Mã định danh kiosk được ghép cặp với thiết bị. */
    private UUID pairedKioskId;

    /** Mã định danh người dùng được giao quản lý thiết bị. */
    private UUID assignedUserId;

    /** Mã nhóm quản lý của thiết bị. */
    private String groupCode;

    /** Tên khu vực lắp đặt thiết bị. */
    private String areaLabel;

    /** Mô tả vị trí lắp đặt cụ thể của thiết bị. */
    private String locationLabel;

    /** Mã băm của thông tin xác thực thiết bị, không lưu giá trị gốc. */
    private String credentialHash;

    /** Phiên bản thông tin xác thực hiện tại của thiết bị. */
    private int credentialVersion;

    /** Thời điểm thông tin xác thực của thiết bị bị thu hồi. */
    private LocalDateTime credentialRevokedAt;

    /** Mã công khai dùng trong quá trình cấp phép thiết bị. */
    private String provisioningPublicId;

    /** Mã băm của token dùng trong quá trình cấp phép thiết bị. */
    private String provisioningTokenHash;

    /** Thời điểm token cấp phép thiết bị hết hạn. */
    private LocalDateTime provisioningExpiresAt;

    /** Mã định danh phần cứng của thiết bị. */
    private String hardwareId;

    /** Cấu hình mong muốn được áp dụng cho thiết bị, lưu dưới dạng JSON. */
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb")
    private String desiredConfig;

    /** Phiên bản cấu hình mong muốn mới nhất. */
    private int desiredConfigVersion;

    /** Phiên bản cấu hình đã được thiết bị áp dụng thành công. */
    private int appliedConfigVersion;

    /** Phiên bản phần cứng của thiết bị. */
    private String hardwareVersion;

    /** Phiên bản phần mềm nhúng của thiết bị. */
    private String firmwareVersion;

    /** Thời điểm thiết bị được hệ thống ghi nhận hoạt động gần nhất. */
    private LocalDateTime lastSeenAt;

    /** Trạng thái sức khỏe gần nhất do thiết bị báo cáo. */
    private String lastHealth;

    /** Mã lỗi gần nhất do thiết bị báo cáo. */
    private String lastErrorCode;

    /** Trạng thái gần nhất do thiết bị báo cáo, lưu dưới dạng JSON. */
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb")
    private String lastReportedState;

    /** Phiên bản dữ liệu đăng ký thiết bị trong hệ thống quản lý. */
    private long registryVersion;

    /** Thời điểm thiết bị được kích hoạt. */
    private LocalDateTime activatedAt;

    /** Thời điểm thiết bị ngừng sử dụng và bị loại khỏi vận hành. */
    private LocalDateTime decommissionedAt;

    /** Thời điểm bản ghi được tạo. */
    @CreatedDate @Column(nullable=false, updatable=false)
    private LocalDateTime createdAt;

    /** Thời điểm bản ghi được cập nhật gần nhất. */
    @LastModifiedDate @Column(nullable=false)
    private LocalDateTime updatedAt;
}
