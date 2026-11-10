package com.dat.ai_receptionist_web.controller.Iot;

import com.dat.ai_receptionist_web.dto.Iot.DeviceDTO;
import com.dat.ai_receptionist_web.service.Iot.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DeviceRuntimeController {
    private final DeviceService service;

    @PostMapping("/device-provisioning/claim")
    public DeviceDTO.ClaimResponse claim(@Valid @RequestBody DeviceDTO.ClaimRequest request) {
        return service.claim(request);
    }

    @GetMapping("/device-runtime/bootstrap")
    public DeviceDTO.BootstrapResponse bootstrap(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        Credentials credentials = credentials(authorization);
        return service.bootstrap(credentials.username(), credentials.password());
    }

    @PostMapping("/internal/fog/device-sync")
    public DeviceDTO.FogSyncResponse sync(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                           @RequestBody DeviceDTO.FogSyncRequest request) {
        Credentials credentials = credentials(authorization);
        return service.sync(credentials.username(), credentials.password(), request);
    }

    private Credentials credentials(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) throw new SecurityException("Missing device credentials");
        String decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
        String[] values = decoded.split(":", 2);
        if (values.length != 2 || values[0].isBlank() || values[1].isBlank()) throw new SecurityException("Invalid device credentials");
        return new Credentials(values[0], values[1]);
    }

    private record Credentials(String username, String password) {}
}
