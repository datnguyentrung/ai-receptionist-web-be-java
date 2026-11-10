package com.dat.ai_receptionist_web.domain.Iot;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @EntityListeners(AuditingEntityListener.class)
@Table(name = "managed_device", schema = "iot")
public class ManagedDevice {
    @Id @GeneratedValue @UuidGenerator @Column(name="device_id") private UUID deviceId;
    @Column(name="device_code", nullable=false, unique=true) private String deviceCode;
    @Column(name="display_name", nullable=false) private String displayName;
    @Column(name="device_type", nullable=false) private String deviceType;
    @Column(name="lifecycle_status", nullable=false) private String lifecycleStatus;
    private Long branchId; private UUID fogGatewayId; private UUID pairedKioskId; private UUID assignedUserId;
    private String groupCode; private String areaLabel; private String locationLabel;
    private String credentialHash; private int credentialVersion; private LocalDateTime credentialRevokedAt;
    private String provisioningPublicId; private String provisioningTokenHash; private LocalDateTime provisioningExpiresAt;
    private String hardwareId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private String desiredConfig;
    private int desiredConfigVersion; private int appliedConfigVersion;
    private String hardwareVersion; private String firmwareVersion; private LocalDateTime lastSeenAt;
    private String lastHealth; private String lastErrorCode;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private String lastReportedState;
    private long registryVersion; private LocalDateTime activatedAt; private LocalDateTime decommissionedAt;
    @CreatedDate @Column(nullable=false, updatable=false) private LocalDateTime createdAt;
    @LastModifiedDate @Column(nullable=false) private LocalDateTime updatedAt;
}
