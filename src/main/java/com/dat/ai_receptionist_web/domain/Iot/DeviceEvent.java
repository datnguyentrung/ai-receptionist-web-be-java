package com.dat.ai_receptionist_web.domain.Iot;


import com.dat.ai_receptionist_web.domain.Base.TenantScopedEntity;import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sự kiện vận hành, cảnh báo hoặc lỗi do thiết bị IoT phát sinh.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="device_event", schema="iot")
public class DeviceEvent extends TenantScopedEntity {
    /** Mã định danh duy nhất của sự kiện thiết bị. */
    @Id @GeneratedValue @UuidGenerator @Column(name="device_event_id")
    private UUID deviceEventId;

    /** Mã định danh của thiết bị phát sinh sự kiện. */
    @Column(nullable=false)
    private UUID deviceId;

    /** Nhóm hoặc loại sự kiện thiết bị. */
    @Column(nullable=false)
    private String eventKind;

    /** Mã nghiệp vụ của sự kiện thiết bị. */
    @Column(nullable=false)
    private String eventCode;

    /** Mức độ nghiêm trọng của sự kiện. */
    private String severity;

    /** Trạng thái cảnh báo hiện tại của sự kiện. */
    private String alertStatus;

    /** Nội dung mô tả sự kiện thiết bị. */
    @Column(nullable=false)
    private String message;

    /** Dữ liệu chi tiết bổ sung của sự kiện, lưu dưới dạng JSON. */
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb")
    private String details;

    /** Mã định danh người dùng đã tạo hoặc ghi nhận sự kiện. */
    private UUID createdByUserId;

    /** Thời điểm sự kiện thực tế xảy ra. */
    @Column(nullable=false)
    private LocalDateTime occurredAt;

    /** Mã định danh người dùng đã xác nhận cảnh báo. */
    private UUID acknowledgedByUserId;

    /** Thời điểm cảnh báo được xác nhận. */
    private LocalDateTime acknowledgedAt;

    /** Mã định danh người dùng đã xử lý sự kiện. */
    private UUID resolvedByUserId;

    /** Thời điểm sự kiện được xử lý xong. */
    private LocalDateTime resolvedAt;

    /** Ghi chú về cách xử lý sự kiện. */
    private String resolutionNote;
}
