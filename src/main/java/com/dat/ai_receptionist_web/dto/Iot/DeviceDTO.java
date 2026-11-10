package com.dat.ai_receptionist_web.dto.Iot;
import jakarta.validation.constraints.*; import java.time.LocalDateTime; import java.util.*;
public final class DeviceDTO { private DeviceDTO() {}
 public record CreateRequest(@NotBlank String displayName,@NotBlank String deviceType, Long branchId, UUID fogGatewayId, UUID pairedKioskId, UUID assignedUserId, String groupCode, String areaLabel, String locationLabel, String hardwareVersion, String firmwareVersion) {}
 public record UpdateRequest(@NotBlank String displayName, Long branchId, UUID fogGatewayId, UUID pairedKioskId, String groupCode, String areaLabel, String locationLabel) {}
 public record LifecycleRequest(@NotBlank String targetStatus, String note) {}
 public record ConfigurationRequest(@NotBlank String desiredConfig, @PositiveOrZero int expectedConfigVersion) {}
 public record ProvisioningCodeResponse(String code, LocalDateTime expiresAt) {}
 public record ClaimRequest(@NotBlank String code, @NotBlank String deviceType, String hardwareId, String installationId, String firmwareVersion, String hardwareVersion) {}
 public record ClaimResponse(String deviceCode,String secret,String lifecycleStatus, RuntimeProfile runtime) {}
 public record RuntimeProfile(String mqttWsUrl, String mqttTcpHost, Integer mqttTcpPort, String fogHttpUrl, String commandTopic, String heartbeatTopic, int heartbeatSeconds, String desiredConfig, int desiredConfigVersion) {}
 public record BootstrapResponse(String deviceCode, String deviceType, String lifecycleStatus, RuntimeProfile runtime) {}
 public record Heartbeat(String deviceCode, LocalDateTime observedAt, String hardwareVersion, String firmwareVersion, Integer appliedConfigVersion, String health, String errorCode, String state) {}
 public record FogSyncRequest(Long knownRegistryVersion, List<Heartbeat> heartbeats) {}
 public record RegistryDevice(String deviceCode,String deviceType,String lifecycleStatus,String credentialHash,String pairedKioskCode,String desiredConfig,int desiredConfigVersion) {}
 public record FogSyncResponse(long registryVersion,boolean registryChanged,String gatewayLifecycleStatus,List<RegistryDevice> devices) {}
 public record EventResponse(UUID deviceEventId, UUID deviceId, String eventKind, String eventCode, String severity, String alertStatus, String message, LocalDateTime occurredAt, String resolutionNote) {}
 public record Response(UUID deviceId,String deviceCode,String displayName,String deviceType,String lifecycleStatus,String connectivity,String lastHealth,Long branchId,UUID fogGatewayId,UUID pairedKioskId,String groupCode,String areaLabel,String locationLabel,String firmwareVersion,String hardwareVersion,LocalDateTime lastSeenAt,String desiredConfig,int desiredConfigVersion) {}
}
