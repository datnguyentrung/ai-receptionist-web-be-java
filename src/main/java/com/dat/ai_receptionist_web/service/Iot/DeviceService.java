package com.dat.ai_receptionist_web.service.Iot;

import com.dat.ai_receptionist_web.domain.Iot.DeviceEvent;
import com.dat.ai_receptionist_web.domain.Iot.ManagedDevice;
import com.dat.ai_receptionist_web.dto.Iot.DeviceDTO;
import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.repository.Iot.DeviceEventRepository;
import com.dat.ai_receptionist_web.repository.Iot.ManagedDeviceRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRepository;
import com.dat.ai_receptionist_web.repository.Notification.NotificationRecipientRepository;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 120;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> TYPES = Set.of("FOG_GATEWAY", "KIOSK", "ESP32");
    private final ManagedDeviceRepository devices;
    private final DeviceEventRepository events;
    private final PasswordEncoder encoder;
    private final NotificationService notifications;
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository notificationRecipientRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DeviceDTO.Response create(DeviceDTO.CreateRequest r) {
        String code = nextDeviceCode(r.deviceType());
        validateRelations(r.deviceType(), r.branchId(), r.fogGatewayId(), r.pairedKioskId());
        ManagedDevice d = ManagedDevice.builder().deviceCode(code).displayName(r.displayName().trim())
                .deviceType(r.deviceType()).lifecycleStatus("REGISTERED").branchId(r.branchId())
                .fogGatewayId(r.fogGatewayId()).pairedKioskId(r.pairedKioskId()).assignedUserId(r.assignedUserId())
                .groupCode(r.groupCode()).areaLabel(r.areaLabel()).locationLabel(r.locationLabel())
                .hardwareVersion(r.hardwareVersion()).firmwareVersion(r.firmwareVersion()).desiredConfig("{}")
                .lastReportedState("{}").lastHealth("UNKNOWN").desiredConfigVersion(1).registryVersion(1).build();
        d = devices.save(d); event(d, "LIFECYCLE", "REGISTERED", "INFO", null, "Device registered"); return response(d);
    }

    @Transactional
    public DeviceDTO.Response update(UUID id, DeviceDTO.UpdateRequest r) {
        ManagedDevice d = get(id);
        UUID oldGatewayId = d.getFogGatewayId();
        validateRelations(r.displayName() == null ? d.getDeviceType() : d.getDeviceType(), r.branchId(), r.fogGatewayId(), r.pairedKioskId());
        if (hasDependents(d.getDeviceId()) && (!Objects.equals(oldGatewayId, r.fogGatewayId()) || !Objects.equals(d.getBranchId(), r.branchId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Move or reassign dependent devices before changing this device");
        }
        d.setDisplayName(r.displayName().trim()); d.setBranchId(r.branchId()); d.setFogGatewayId(r.fogGatewayId()); d.setPairedKioskId(r.pairedKioskId());
        d.setGroupCode(r.groupCode()); d.setAreaLabel(r.areaLabel()); d.setLocationLabel(r.locationLabel());
        touchGatewayById(oldGatewayId); touchGateway(d);
        event(d, "CONFIG", "UPDATED", "INFO", null, "Device assignment updated");
        return response(d);
    }

    @Transactional
    public void delete(UUID id) {
        ManagedDevice d = get(id);
        if (hasDependents(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Device still has dependent devices");
        UUID parentGateway = d.getFogGatewayId();
        List<String> alertIds = events.findByDeviceIdOrderByOccurredAtDesc(id).stream()
                .filter(e -> "ALERT".equals(e.getEventKind())).map(e -> e.getDeviceEventId().toString()).toList();
        if (!alertIds.isEmpty()) {
            var notificationsToRemove = notificationRepository.findAll().stream()
                    .filter(n -> "DEVICE_ALERT".equals(n.getReferenceType()) && alertIds.contains(n.getReferenceId()))
                    .map(n -> n.getNotificationId()).toList();
            if (!notificationsToRemove.isEmpty()) notificationRecipientRepository.deleteByNotificationIds(notificationsToRemove);
            notificationRepository.deleteByReferenceTypeAndReferenceIdIn("DEVICE_ALERT", alertIds);
        }
        devices.delete(d);
        touchGatewayById(parentGateway);
    }

    @Transactional
    public DeviceDTO.ProvisioningCodeResponse provisioningCode(UUID id) {
        ManagedDevice d = get(id);
        if ("DECOMMISSIONED".equals(d.getLifecycleStatus())) throw new IllegalArgumentException("Device is decommissioned");
        String publicId = random(9), secret = random(24);
        d.setProvisioningPublicId(publicId); d.setProvisioningTokenHash(encoder.encode(secret));
        d.setProvisioningExpiresAt(LocalDateTime.now().plusMinutes(10)); touchGateway(d);
        return new DeviceDTO.ProvisioningCodeResponse(publicId + "." + secret, d.getProvisioningExpiresAt());
    }

    @Transactional
    public DeviceDTO.ClaimResponse claim(DeviceDTO.ClaimRequest r) {
        String[] code = r.code().trim().split("\\.", 2);
        if (code.length != 2) throw new IllegalArgumentException("Provisioning code is invalid");
        ManagedDevice d = devices.findByProvisioningPublicId(code[0]).orElseThrow(() -> new IllegalArgumentException("Provisioning code is invalid or expired"));
        if (!d.getDeviceType().equals(r.deviceType()) || expired(d) || !encoder.matches(code[1], d.getProvisioningTokenHash())) throw new IllegalArgumentException("Provisioning code is invalid or expired");
        if (r.hardwareId() != null && !r.hardwareId().isBlank()) {
            String hardwareId = r.hardwareId().trim();
            devices.findByHardwareId(hardwareId).filter(other -> !other.getDeviceId().equals(d.getDeviceId())).ifPresent(other -> { throw new IllegalArgumentException("Hardware is already registered"); });
            d.setHardwareId(hardwareId);
        }
        String secret = random(32);
        d.setCredentialHash(encoder.encode(secret)); d.setCredentialVersion(d.getCredentialVersion() + 1); d.setCredentialRevokedAt(null);
        d.setProvisioningPublicId(null); d.setProvisioningTokenHash(null); d.setProvisioningExpiresAt(null); d.setLifecycleStatus("PROVISIONED");
        if (r.hardwareVersion() != null) d.setHardwareVersion(r.hardwareVersion()); if (r.firmwareVersion() != null) d.setFirmwareVersion(r.firmwareVersion());
        event(d, "AUTH", "PROVISIONED", "INFO", null, "Device provisioning completed"); touchGateway(d);
        return new DeviceDTO.ClaimResponse(d.getDeviceCode(), secret, d.getLifecycleStatus(), runtime(d));
    }

    @Transactional(readOnly = true)
    public DeviceDTO.BootstrapResponse bootstrap(String deviceCode, String secret) {
        ManagedDevice d = authenticate(deviceCode, secret);
        return new DeviceDTO.BootstrapResponse(d.getDeviceCode(), d.getDeviceType(), d.getLifecycleStatus(), runtime(d));
    }

    @Transactional
    public DeviceDTO.Response lifecycle(UUID id, DeviceDTO.LifecycleRequest r) {
        ManagedDevice d = get(id); String target = r.targetStatus().trim(); transition(d.getLifecycleStatus(), target);
        String previous = d.getLifecycleStatus(); d.setLifecycleStatus(target);
        if ("ACTIVE".equals(target)) d.setActivatedAt(LocalDateTime.now());
        if ("DECOMMISSIONED".equals(target)) revokeInternal(d, "Device decommissioned");
        event(d, "LIFECYCLE", target, "INFO", null, previous + " -> " + target + note(r.note())); touchGateway(d); return response(d);
    }

    @Transactional
    public void revoke(UUID id) { ManagedDevice d = get(id); if (!"DECOMMISSIONED".equals(d.getLifecycleStatus())) { revokeInternal(d, "Credential revoked by administrator"); d.setLifecycleStatus("SUSPENDED"); touchGateway(d); } }

    @Transactional
    public void configuration(UUID id, DeviceDTO.ConfigurationRequest r) {
        ManagedDevice d = get(id); if (r.expectedConfigVersion() != d.getDesiredConfigVersion()) throw new IllegalStateException("Configuration changed by another administrator");
        try { objectMapper.readTree(r.desiredConfig()); } catch (Exception e) { throw new IllegalArgumentException("desiredConfig must be valid JSON", e); }
        d.setDesiredConfig(r.desiredConfig()); d.setDesiredConfigVersion(d.getDesiredConfigVersion() + 1);
        event(d, "CONFIG", "UPDATED", "INFO", null, "Device configuration updated"); touchGateway(d);
    }

    @Transactional
    public void heartbeat(DeviceDTO.Heartbeat h) {
        ManagedDevice d = devices.findByDeviceCode(h.deviceCode()).orElseThrow(() -> new IllegalArgumentException("Unknown device"));
        if (isStale(d, h)) return;
        updateHeartbeat(d, h);
        events.findOpenAlert(d.getDeviceId(), "DEVICE_OFFLINE").ifPresent(this::autoResolve);
    }

    @Transactional
    public DeviceDTO.FogSyncResponse sync(String gatewayCode, String secret, DeviceDTO.FogSyncRequest r) {
        ManagedDevice gateway = authenticate(gatewayCode, secret);
        if (!"FOG_GATEWAY".equals(gateway.getDeviceType())) throw new SecurityException("Device is not a fog gateway");
        List<DeviceDTO.Heartbeat> heartbeats = new ArrayList<>();
        heartbeats.add(new DeviceDTO.Heartbeat(gatewayCode, LocalDateTime.now(), null, null, null, "HEALTHY", null, null));
        if (r.heartbeats() != null) heartbeats.addAll(r.heartbeats());
        applyHeartbeats(heartbeats, gateway);
        boolean changed = r.knownRegistryVersion() == null || r.knownRegistryVersion() != gateway.getRegistryVersion();
        if (!changed) return new DeviceDTO.FogSyncResponse(gateway.getRegistryVersion(), false, gateway.getLifecycleStatus(), null);
        List<ManagedDevice> registryDevices = devices.findByFogGatewayId(gateway.getDeviceId());
        Map<UUID, String> pairedKioskCodes = pairedKioskCodes(registryDevices);
        List<DeviceDTO.RegistryDevice> registry = registryDevices.stream()
                .map(d -> registryDevice(d, pairedKioskCodes))
                .toList();
        return new DeviceDTO.FogSyncResponse(gateway.getRegistryVersion(), true, gateway.getLifecycleStatus(), registry);
    }

    @Transactional
    public void checkOffline() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        for (ManagedDevice d : devices.findByLifecycleStatusAndLastSeenAtBefore("ACTIVE", cutoff)) {
            if (offlineGateway(d, cutoff)) continue;
            if (events.findOpenAlert(d.getDeviceId(), "DEVICE_OFFLINE").isEmpty()) event(d, "ALERT", "DEVICE_OFFLINE", "WARNING", "OPEN", "No heartbeat for 120 seconds");
        }
    }

    @Transactional public void acknowledge(UUID id, UUID actor) { DeviceEvent e = event(id); if (!"ALERT".equals(e.getEventKind()) || !"OPEN".equals(e.getAlertStatus())) throw new IllegalArgumentException("Alert cannot be acknowledged"); e.setAlertStatus("ACKNOWLEDGED"); e.setAcknowledgedAt(LocalDateTime.now()); e.setAcknowledgedByUserId(actor); }
    @Transactional public void resolve(UUID id, UUID actor, String note) { if (note == null || note.isBlank()) throw new IllegalArgumentException("Resolution note is required"); DeviceEvent e = event(id); if (!"ALERT".equals(e.getEventKind()) || "RESOLVED".equals(e.getAlertStatus())) throw new IllegalArgumentException("Alert cannot be resolved"); e.setAlertStatus("RESOLVED"); e.setResolvedAt(LocalDateTime.now()); e.setResolvedByUserId(actor); e.setResolutionNote(note.trim()); }
    @Transactional(readOnly = true) public Map<String, Long> summary() { List<ManagedDevice> all = devices.findAll(); return Map.of("total", (long) all.size(), "online", all.stream().filter(d -> "ONLINE".equals(connectivity(d))).count(), "offline", all.stream().filter(d -> "OFFLINE".equals(connectivity(d))).count(), "attention", all.stream().filter(d -> "MAINTENANCE".equals(d.getLifecycleStatus()) || "FAULT".equals(d.getLastHealth())).count()); }
    @Transactional(readOnly = true) public List<DeviceDTO.Response> list() { return devices.findAll().stream().map(this::response).toList(); }
    @Transactional(readOnly = true) public List<DeviceDTO.EventResponse> listEvents() { return events.findAll().stream().sorted(Comparator.comparing(DeviceEvent::getOccurredAt).reversed()).map(e -> new DeviceDTO.EventResponse(e.getDeviceEventId(), e.getDeviceId(), e.getEventKind(), e.getEventCode(), e.getSeverity(), e.getAlertStatus(), e.getMessage(), e.getOccurredAt(), e.getResolutionNote())).toList(); }
    @Transactional(readOnly = true) public DeviceDTO.Response detail(UUID id) { return response(get(id)); }

    private void validateRelations(String type, Long branchId, UUID gatewayId, UUID kioskId) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unsupported device type");
        if ("FOG_GATEWAY".equals(type) && (gatewayId != null || kioskId != null)) throw new IllegalArgumentException("Fog gateway cannot have parent device");
        if ("KIOSK".equals(type) && (branchId == null || gatewayId == null)) throw new IllegalArgumentException("Kiosk requires branch and fog gateway");
        if ("ESP32".equals(type)) { if (branchId == null || gatewayId == null || kioskId == null) throw new IllegalArgumentException("ESP32 requires branch, fog gateway and kiosk"); ManagedDevice k = get(kioskId); if (!"KIOSK".equals(k.getDeviceType()) || !branchId.equals(k.getBranchId()) || !gatewayId.equals(k.getFogGatewayId())) throw new IllegalArgumentException("ESP32 pairing is outside kiosk branch/gateway"); }
    }
    private boolean hasDependents(UUID id) { return devices.existsByFogGatewayIdOrPairedKioskId(id, id); }
    private void transition(String from, String to) { boolean ok = switch (from) { case "REGISTERED" -> Set.of("PROVISIONED", "DECOMMISSIONED").contains(to); case "PROVISIONED" -> Set.of("ACTIVE", "SUSPENDED", "DECOMMISSIONED").contains(to); case "ACTIVE", "MAINTENANCE" -> Set.of("ACTIVE", "MAINTENANCE", "SUSPENDED", "DECOMMISSIONED").contains(to); case "SUSPENDED" -> Set.of("PROVISIONED", "ACTIVE", "DECOMMISSIONED").contains(to); default -> false; }; if (!ok) throw new IllegalArgumentException("Invalid lifecycle transition"); }
    private ManagedDevice authenticate(String code, String secret) { ManagedDevice d = devices.findByDeviceCode(code).orElseThrow(() -> new SecurityException("Device credentials are invalid")); if (d.getCredentialHash() == null || d.getCredentialRevokedAt() != null || !encoder.matches(secret, d.getCredentialHash())) throw new SecurityException("Device credentials are invalid"); return d; }
    private DeviceDTO.RuntimeProfile runtime(ManagedDevice d) { JsonNode own = json(d.getDesiredConfig()), gateway = json(d.getFogGatewayId() == null ? d.getDesiredConfig() : get(d.getFogGatewayId()).getDesiredConfig()); return new DeviceDTO.RuntimeProfile(gateway.path("mqttWsUrl").asText(null), gateway.path("mqttTcpHost").asText(null), gateway.path("mqttTcpPort").isInt() ? gateway.path("mqttTcpPort").intValue() : null, gateway.path("fogHttpUrl").asText(null), "taekwondo/kiosks/" + d.getDeviceCode() + "/commands", "taekwondo/devices/" + d.getDeviceCode() + "/heartbeat", own.path("heartbeatSeconds").asInt(30), d.getDesiredConfig(), d.getDesiredConfigVersion()); }
    private JsonNode json(String value) { try { return objectMapper.readTree(value); } catch (Exception e) { return objectMapper.createObjectNode(); } }
    private boolean offlineGateway(ManagedDevice d, LocalDateTime cutoff) { return d.getFogGatewayId() != null && devices.findById(d.getFogGatewayId()).map(g -> g.getLastSeenAt() == null || g.getLastSeenAt().isBefore(cutoff)).orElse(true); }
    private void applyHeartbeats(List<DeviceDTO.Heartbeat> heartbeats, ManagedDevice gateway) {
        Set<String> deviceCodes = new LinkedHashSet<>();
        for (DeviceDTO.Heartbeat heartbeat : heartbeats) {
            if (heartbeat.deviceCode() == null) throw new IllegalArgumentException("Unknown device");
            deviceCodes.add(heartbeat.deviceCode());
        }

        Map<String, ManagedDevice> devicesByCode = new HashMap<>();
        devicesByCode.put(gateway.getDeviceCode(), gateway);
        Set<String> lookupCodes = new LinkedHashSet<>(deviceCodes);
        lookupCodes.remove(gateway.getDeviceCode());
        if (!lookupCodes.isEmpty()) {
            for (ManagedDevice device : devices.findByDeviceCodeIn(lookupCodes)) {
                devicesByCode.put(device.getDeviceCode(), device);
            }
        }

        Set<UUID> deviceIds = new HashSet<>();
        for (String deviceCode : deviceCodes) {
            ManagedDevice device = devicesByCode.get(deviceCode);
            if (device == null) throw new IllegalArgumentException("Unknown device");
            deviceIds.add(device.getDeviceId());
        }

        Map<UUID, DeviceEvent> openAlerts = new HashMap<>();
        for (DeviceEvent alert : events.findOpenAlerts(deviceIds, "DEVICE_OFFLINE")) {
            openAlerts.put(alert.getDeviceId(), alert);
        }

        for (DeviceDTO.Heartbeat heartbeat : heartbeats) {
            ManagedDevice device = devicesByCode.get(heartbeat.deviceCode());
            if (isStale(device, heartbeat)) continue;
            updateHeartbeat(device, heartbeat);
            DeviceEvent alert = openAlerts.remove(device.getDeviceId());
            if (alert != null) autoResolve(alert);
        }
    }
    private boolean isStale(ManagedDevice d, DeviceDTO.Heartbeat h) {
        LocalDateTime observed = h.observedAt() == null ? LocalDateTime.now() : h.observedAt();
        return d.getLastSeenAt() != null && observed.isBefore(d.getLastSeenAt());
    }
    private void updateHeartbeat(ManagedDevice d, DeviceDTO.Heartbeat h) {
        d.setLastSeenAt(h.observedAt() == null ? LocalDateTime.now() : h.observedAt());
        if (h.hardwareVersion() != null) d.setHardwareVersion(h.hardwareVersion());
        if (h.firmwareVersion() != null) d.setFirmwareVersion(h.firmwareVersion());
        if (h.appliedConfigVersion() != null) d.setAppliedConfigVersion(h.appliedConfigVersion());
        d.setLastHealth(h.health() == null ? "HEALTHY" : h.health());
        d.setLastErrorCode(h.errorCode());
        if (h.state() != null) d.setLastReportedState(h.state());
    }
    private Map<UUID, String> pairedKioskCodes(List<ManagedDevice> registryDevices) {
        Set<UUID> kioskIds = new HashSet<>();
        for (ManagedDevice device : registryDevices) {
            if (device.getPairedKioskId() != null) kioskIds.add(device.getPairedKioskId());
        }
        if (kioskIds.isEmpty()) return Map.of();

        Map<UUID, String> codes = new HashMap<>();
        for (ManagedDevice kiosk : devices.findAllById(kioskIds)) {
            codes.put(kiosk.getDeviceId(), kiosk.getDeviceCode());
        }
        return codes;
    }
    private DeviceDTO.RegistryDevice registryDevice(ManagedDevice d, Map<UUID, String> pairedKioskCodes) {
        String pairedKioskCode = d.getPairedKioskId() == null ? null : pairedKioskCodes.get(d.getPairedKioskId());
        return new DeviceDTO.RegistryDevice(d.getDeviceCode(), d.getDeviceType(), d.getLifecycleStatus(), d.getCredentialHash(), pairedKioskCode, d.getDesiredConfig(), d.getDesiredConfigVersion());
    }
    private void revokeInternal(ManagedDevice d, String message) { d.setCredentialHash(null); d.setCredentialRevokedAt(LocalDateTime.now()); event(d, "AUTH", "REVOKED", "WARNING", null, message); }
    private void touchGateway(ManagedDevice d) { if (d.getFogGatewayId() != null) devices.findById(d.getFogGatewayId()).ifPresent(g -> g.setRegistryVersion(g.getRegistryVersion() + 1)); if ("FOG_GATEWAY".equals(d.getDeviceType())) d.setRegistryVersion(d.getRegistryVersion() + 1); }
    private void touchGatewayById(UUID id) { if (id != null) devices.findById(id).ifPresent(g -> g.setRegistryVersion(g.getRegistryVersion() + 1)); }
    private void event(ManagedDevice d, String kind, String code, String severity, String status, String message) { DeviceEvent e = events.save(DeviceEvent.builder().deviceId(d.getDeviceId()).eventKind(kind).eventCode(code).severity(severity).alertStatus(status).message(message).details("{}").occurredAt(LocalDateTime.now()).build()); if ("ALERT".equals(kind)) notifications.create(new NotificationDTO.CreateRequest("Cảnh báo thiết bị: " + d.getDisplayName(), message, NotificationType.DEVICE_ALERT, "DEVICE_ALERT", e.getDeviceEventId().toString(), "{\"deviceCode\":\"" + d.getDeviceCode() + "\"}", null, null, Set.of("SUPER_ADMIN", "SYSTEM_ADMIN"))); }
    private void autoResolve(DeviceEvent e) { e.setAlertStatus("RESOLVED"); e.setResolvedAt(LocalDateTime.now()); e.setResolutionNote("AUTO_RECOVERED"); }
    private boolean expired(ManagedDevice d) { return d.getProvisioningTokenHash() == null || d.getProvisioningExpiresAt() == null || !d.getProvisioningExpiresAt().isAfter(LocalDateTime.now()); }
    private ManagedDevice get(UUID id) { return devices.findById(id).orElseThrow(() -> new NoSuchElementException("Device not found")); }
    private DeviceEvent event(UUID id) { return events.findById(id).orElseThrow(() -> new NoSuchElementException("Device event not found")); }
    private DeviceDTO.Response response(ManagedDevice d) { return new DeviceDTO.Response(d.getDeviceId(), d.getDeviceCode(), d.getDisplayName(), d.getDeviceType(), d.getLifecycleStatus(), connectivity(d), d.getLastHealth(), d.getBranchId(), d.getFogGatewayId(), d.getPairedKioskId(), d.getGroupCode(), d.getAreaLabel(), d.getLocationLabel(), d.getFirmwareVersion(), d.getHardwareVersion(), d.getLastSeenAt(), d.getDesiredConfig(), d.getDesiredConfigVersion()); }
    private String connectivity(ManagedDevice d) { return d.getLastSeenAt() == null ? "UNKNOWN" : d.getLastSeenAt().isAfter(LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS)) ? "ONLINE" : "OFFLINE"; }
    private String random(int size) { byte[] bytes = new byte[size]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String nextDeviceCode(String type) {
        String prefix = type.toLowerCase(Locale.ROOT).replace('_', '-');
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = prefix + "-" + random(6).toLowerCase(Locale.ROOT);
            if (devices.findByDeviceCode(candidate).isEmpty()) return candidate;
        }
        throw new IllegalStateException("Unable to allocate a unique device code");
    }
    private String note(String value) { return value == null || value.isBlank() ? "" : ": " + value.trim(); }
}
