package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.scan.FaceCheckInScanDTO;
import com.dat.ai_receptionist_web.service.Training.scan.FaceCheckInScanService;
import com.dat.ai_receptionist_web.service.Training.scan.FogInternalAuthenticator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/face-check-in-scans")
@RequiredArgsConstructor
public class FaceCheckInInternalController {
    private final FaceCheckInScanService service;
    private final FogInternalAuthenticator authenticator;

    @PostMapping("/{requestId}/validate-upload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validateUpload(
            @RequestHeader(name = "X-Fog-Secret", required = false) String secret,
            @PathVariable UUID requestId,
            @Valid @RequestBody FaceCheckInScanDTO.UploadValidationRequest request) {
        authenticator.requireValid(secret);
        service.validateUpload(requestId, request);
    }

    @PostMapping("/{requestId}/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void event(
            @RequestHeader(name = "X-Fog-Secret", required = false) String secret,
            @PathVariable UUID requestId,
            @Valid @RequestBody FaceCheckInScanDTO.EventRequest request) {
        authenticator.requireValid(secret);
        service.recordFogEvent(requestId, request);
    }

    @PostMapping("/{requestId}/embedding")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void embedding(
            @RequestHeader(name = "X-Fog-Secret", required = false) String secret,
            @PathVariable UUID requestId,
            @Valid @RequestBody FaceCheckInScanDTO.EmbeddingRequest request) {
        authenticator.requireValid(secret);
        service.submitEmbedding(requestId, request);
    }
}
