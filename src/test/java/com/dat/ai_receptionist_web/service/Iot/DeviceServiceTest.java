package com.dat.ai_receptionist_web.service.Iot;

import com.dat.ai_receptionist_web.domain.Iot.DeviceEvent;
import com.dat.ai_receptionist_web.domain.Iot.ManagedDevice;
import com.dat.ai_receptionist_web.dto.Iot.DeviceDTO;
import com.dat.ai_receptionist_web.repository.Iot.DeviceEventRepository;
import com.dat.ai_receptionist_web.repository.Iot.ManagedDeviceRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    @Mock ManagedDeviceRepository devices;
    @Mock DeviceEventRepository events;
    @Mock PasswordEncoder encoder;
    @Mock NotificationService notifications;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationRecipientRepository notificationRecipientRepository;
    @Mock ObjectMapper objectMapper;
    @InjectMocks DeviceService service;

    @Test
    void syncLoadsHeartbeatDevicesAndAlertsInBatches() {
        UUID gatewayId = UUID.randomUUID();
        UUID firstDeviceId = UUID.randomUUID();
        UUID secondDeviceId = UUID.randomUUID();
        ManagedDevice gateway = device(gatewayId, "fog-gateway", "FOG_GATEWAY", "ACTIVE");
        ManagedDevice firstDevice = device(firstDeviceId, "esp32-first", "ESP32", "ACTIVE");
        ManagedDevice secondDevice = device(secondDeviceId, "esp32-second", "ESP32", "ACTIVE");
        DeviceEvent offlineAlert = DeviceEvent.builder()
                .deviceEventId(UUID.randomUUID())
                .deviceId(firstDeviceId)
                .eventKind("ALERT")
                .eventCode("DEVICE_OFFLINE")
                .severity("WARNING")
                .alertStatus("OPEN")
                .message("No heartbeat")
                .occurredAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(devices.findByDeviceCode("fog-gateway")).thenReturn(Optional.of(gateway));
        when(encoder.matches("secret", "credential-hash")).thenReturn(true);
        when(devices.findByDeviceCodeIn(Set.of("esp32-first", "esp32-second")))
                .thenReturn(List.of(firstDevice, secondDevice));
        when(events.findOpenAlerts(Set.of(gatewayId, firstDeviceId, secondDeviceId), "DEVICE_OFFLINE"))
                .thenReturn(List.of(offlineAlert));

        LocalDateTime firstObservedAt = LocalDateTime.now().minusSeconds(2);
        LocalDateTime secondObservedAt = LocalDateTime.now().minusSeconds(1);
        DeviceDTO.FogSyncResponse response = service.sync("fog-gateway", "secret",
                new DeviceDTO.FogSyncRequest(30L, List.of(
                        new DeviceDTO.Heartbeat("esp32-first", firstObservedAt, "hw-1", "fw-1", 4, "HEALTHY", null, "{\"online\":true}"),
                        new DeviceDTO.Heartbeat("esp32-second", secondObservedAt, null, null, null, "DEGRADED", "WIFI_WEAK", null)
                )));

        assertThat(response.registryChanged()).isFalse();
        assertThat(firstDevice.getLastSeenAt()).isEqualTo(firstObservedAt);
        assertThat(firstDevice.getAppliedConfigVersion()).isEqualTo(4);
        assertThat(secondDevice.getLastSeenAt()).isEqualTo(secondObservedAt);
        assertThat(secondDevice.getLastHealth()).isEqualTo("DEGRADED");
        assertThat(offlineAlert.getAlertStatus()).isEqualTo("RESOLVED");
        assertThat(offlineAlert.getResolutionNote()).isEqualTo("AUTO_RECOVERED");

        verify(devices).findByDeviceCode("fog-gateway");
        verify(devices).findByDeviceCodeIn(Set.of("esp32-first", "esp32-second"));
        verify(events).findOpenAlerts(Set.of(gatewayId, firstDeviceId, secondDeviceId), "DEVICE_OFFLINE");
        verify(events, never()).findOpenAlert(any(), anyString());
    }

    @Test
    void syncLoadsPairedKiosksInOneLookupWhenRegistryChanges() {
        UUID gatewayId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID pairedKioskId = UUID.randomUUID();
        ManagedDevice gateway = device(gatewayId, "fog-gateway", "FOG_GATEWAY", "ACTIVE");
        ManagedDevice device = device(deviceId, "esp32-first", "ESP32", "ACTIVE");
        device.setPairedKioskId(pairedKioskId);
        ManagedDevice pairedKiosk = device(pairedKioskId, "kiosk-first", "KIOSK", "ACTIVE");

        when(devices.findByDeviceCode("fog-gateway")).thenReturn(Optional.of(gateway));
        when(encoder.matches("secret", "credential-hash")).thenReturn(true);
        when(events.findOpenAlerts(Set.of(gatewayId), "DEVICE_OFFLINE")).thenReturn(List.of());
        when(devices.findByFogGatewayId(gatewayId)).thenReturn(List.of(device));
        when(devices.findAllById(Set.of(pairedKioskId))).thenReturn(List.of(pairedKiosk));

        DeviceDTO.FogSyncResponse response = service.sync("fog-gateway", "secret",
                new DeviceDTO.FogSyncRequest(29L, List.of()));

        assertThat(response.registryChanged()).isTrue();
        assertThat(response.devices()).hasSize(1);
        assertThat(response.devices().getFirst().pairedKioskCode()).isEqualTo("kiosk-first");
        verify(devices, never()).findByDeviceCodeIn(any());
        verify(devices).findAllById(Set.of(pairedKioskId));
        verify(devices, never()).findById(any());
    }

    private ManagedDevice device(UUID id, String code, String type, String lifecycleStatus) {
        return ManagedDevice.builder()
                .deviceId(id)
                .deviceCode(code)
                .displayName(code)
                .deviceType(type)
                .lifecycleStatus(lifecycleStatus)
                .credentialHash("credential-hash")
                .desiredConfig("{}")
                .desiredConfigVersion(1)
                .registryVersion(30)
                .build();
    }
}
