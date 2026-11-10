package com.dat.ai_receptionist_web.controller.Iot;
import com.dat.ai_receptionist_web.dto.Iot.DeviceDTO; import com.dat.ai_receptionist_web.service.Iot.DeviceService; import com.dat.ai_receptionist_web.util.SecurityUtil; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/devices") @RequiredArgsConstructor public class DeviceController {private final DeviceService service;
 @GetMapping @PreAuthorize("hasAuthority('DEVICE_READ')") public List<DeviceDTO.Response> list(){return service.list();}
 @GetMapping("/{id}") @PreAuthorize("hasAuthority('DEVICE_READ')") public DeviceDTO.Response detail(@PathVariable UUID id){return service.detail(id);}
 @GetMapping("/summary") @PreAuthorize("hasAuthority('DEVICE_READ')") public Map<String,Long> summary(){return service.summary();}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('DEVICE_CREATE')") public DeviceDTO.Response create(@Valid @RequestBody DeviceDTO.CreateRequest r){return service.create(r);}
 @PutMapping("/{id}") @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public DeviceDTO.Response update(@PathVariable UUID id,@Valid @RequestBody DeviceDTO.UpdateRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('DEVICE_DELETE')") public void delete(@PathVariable UUID id){service.delete(id);}
 @PostMapping({"/{id}/provisioning-code", "/{id}/provisioning-codes"}) @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public DeviceDTO.ProvisioningCodeResponse code(@PathVariable UUID id){return service.provisioningCode(id);}
 @PatchMapping("/{id}/lifecycle") @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public DeviceDTO.Response lifecycle(@PathVariable UUID id,@Valid @RequestBody DeviceDTO.LifecycleRequest r){return service.lifecycle(id,r);}
 @PutMapping("/{id}/configuration") @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public void config(@PathVariable UUID id,@Valid @RequestBody DeviceDTO.ConfigurationRequest r){service.configuration(id,r);}
 @PostMapping("/{id}/credentials/revoke") @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public void revoke(@PathVariable UUID id){service.revoke(id);}
 @PatchMapping("/events/{id}/acknowledge") @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public void acknowledge(@PathVariable UUID id){service.acknowledge(id,SecurityUtil.getCurrentUserId().orElseThrow());}
 @PatchMapping("/events/{id}/resolve") @PreAuthorize("hasAuthority('DEVICE_UPDATE')") public void resolve(@PathVariable UUID id,@RequestBody Map<String,String> body){service.resolve(id,SecurityUtil.getCurrentUserId().orElseThrow(),body.get("note"));}
}
