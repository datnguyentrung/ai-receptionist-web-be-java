package com.dat.ai_receptionist_web.dto.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FaceCheckInScanDTO {
    private FaceCheckInScanDTO() {}

    public record RegisterRequest(
            @NotNull UUID requestId,
            @NotBlank @Size(max = 128) String deviceId
    ) {}

    public record RegisterResponse(
            UUID requestId,
            String deviceId,
            String uploadTicket,
            Instant expiresAt
    ) {}

    public record UploadValidationRequest(
            @NotBlank @Size(max = 128) String deviceId,
            @NotBlank String uploadTicket
    ) {}

    public record EventRequest(
            @NotBlank @Size(max = 128) String deviceId,
            @NotNull FaceCheckInStage stage,
            @Valid FaceCheckInResponse.ErrorSummary error
    ) {}

    public record EmbeddingRequest(
            @NotBlank @Size(max = 128) String deviceId,
            @NotNull @Size(min = 512, max = 512) List<Float> embedding
    ) {}

    public record EventResponse(
            long eventId,
            UUID requestId,
            String deviceId,
            FaceCheckInStage stage,
            Instant occurredAt,
            FaceCheckInResponse result,
            FaceCheckInResponse.ErrorSummary error
    ) {}
}
