package com.dat.ai_receptionist_web.domain.Iot;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name="device_event", schema="iot")
public class DeviceEvent {
 @Id @GeneratedValue @UuidGenerator @Column(name="device_event_id") private UUID deviceEventId;
 @Column(nullable=false) private UUID deviceId; @Column(nullable=false) private String eventKind;
 @Column(nullable=false) private String eventCode; private String severity; private String alertStatus;
 @Column(nullable=false) private String message; @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private String details;
 private UUID createdByUserId; @Column(nullable=false) private LocalDateTime occurredAt;
 private UUID acknowledgedByUserId; private LocalDateTime acknowledgedAt;
 private UUID resolvedByUserId; private LocalDateTime resolvedAt; private String resolutionNote;
}
