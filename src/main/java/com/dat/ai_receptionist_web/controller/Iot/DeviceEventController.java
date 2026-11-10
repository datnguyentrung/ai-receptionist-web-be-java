package com.dat.ai_receptionist_web.controller.Iot;

import com.dat.ai_receptionist_web.dto.Iot.DeviceDTO;
import com.dat.ai_receptionist_web.service.Iot.DeviceService;
import com.dat.ai_receptionist_web.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device-events")
@RequiredArgsConstructor
public class DeviceEventController {
 private final DeviceService service;
 @GetMapping @PreAuthorize("hasAuthority('DEVICE_READ')")
 public List<DeviceDTO.EventResponse> list() { return service.listEvents(); }
 @PatchMapping("/{id}/acknowledge") @PreAuthorize("hasAuthority('DEVICE_UPDATE')")
 public void acknowledge(@PathVariable UUID id) { service.acknowledge(id, SecurityUtil.getCurrentUserId().orElseThrow()); }
 @PatchMapping("/{id}/resolve") @PreAuthorize("hasAuthority('DEVICE_UPDATE')")
 public void resolve(@PathVariable UUID id, @RequestBody Map<String, String> body) { service.resolve(id, SecurityUtil.getCurrentUserId().orElseThrow(), body.get("note")); }
}
