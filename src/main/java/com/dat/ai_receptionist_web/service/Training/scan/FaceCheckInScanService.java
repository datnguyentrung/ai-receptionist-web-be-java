package com.dat.ai_receptionist_web.service.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInEvent;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInScan;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.dto.Training.scan.FaceCheckInScanDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.scan.FaceCheckInEventRepository;
import com.dat.ai_receptionist_web.repository.Training.scan.FaceCheckInScanRepository;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.FaceIdentificationService;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceCheckInScanService {
    private static final EnumSet<FaceCheckInStage> TERMINAL_STAGES = EnumSet.of(
            FaceCheckInStage.SUCCEEDED,
            FaceCheckInStage.REJECTED,
            FaceCheckInStage.FAILED,
            FaceCheckInStage.EXPIRED);
    private static final EnumSet<FaceCheckInStage> FOG_STAGES = EnumSet.of(
            FaceCheckInStage.IMAGE_RECEIVED,
            FaceCheckInStage.FACE_DETECTED,
            FaceCheckInStage.EMBEDDING_READY,
            FaceCheckInStage.REJECTED,
            FaceCheckInStage.FAILED);

    private final FaceCheckInScanRepository scanRepository;
    private final FaceCheckInEventRepository eventRepository;
    private final CurrentAccessContextResolver accessContextResolver;
    private final FaceIdentificationService faceIdentificationService;
    private final AttendanceCommandService attendanceCommandService;
    private final FaceCheckInEventStream eventStream;
    private final FaceCheckInExpiryRegistry expiryRegistry;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.face-check-in.scan-expire-after:30s}")
    private Duration expireAfter;

    @Value("${app.face-check-in.upload-ticket-bytes:32}")
    private int uploadTicketBytes;

    @Transactional
    public FaceCheckInScanDTO.RegisterResponse register(FaceCheckInScanDTO.RegisterRequest request) {
        if (scanRepository.existsById(request.requestId())) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_SCAN_DUPLICATE);
        }
        Instant now = Instant.now();
        String deviceId = request.deviceId().trim();
        String ticket = newUploadTicket();
        FaceCheckInScan scan = FaceCheckInScan.builder()
                .requestId(request.requestId())
                .deviceId(deviceId)
                .userId(accessContextResolver.current().userId())
                .uploadTicketHash(hash(ticket))
                .stage(FaceCheckInStage.READY)
                .expiresAt(now.plus(expireAfter))
                .createdAt(now)
                .updatedAt(now)
                .build();
        scanRepository.save(scan);
        appendEvent(scan, FaceCheckInStage.READY, null, null);
        afterCommit(() -> expiryRegistry.track(scan.getRequestId(), scan.getExpiresAt()));
        return new FaceCheckInScanDTO.RegisterResponse(
                scan.getRequestId(), scan.getDeviceId(), ticket, scan.getExpiresAt());
    }

    @Transactional
    public void validateUpload(UUID requestId, FaceCheckInScanDTO.UploadValidationRequest request) {
        FaceCheckInScan scan = requireActiveScan(requestId, request.deviceId());
        if (!MessageDigest.isEqual(
                scan.getUploadTicketHash().getBytes(StandardCharsets.US_ASCII),
                hash(request.uploadTicket()).getBytes(StandardCharsets.US_ASCII))) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_UPLOAD_TICKET_INVALID);
        }
        if (scan.getTicketUsedAt() == null) {
            scan.setTicketUsedAt(Instant.now());
            scan.setUpdatedAt(Instant.now());
        }
    }

    @Transactional
    public void recordFogEvent(UUID requestId, FaceCheckInScanDTO.EventRequest request) {
        if (!FOG_STAGES.contains(request.stage())) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        }
        FaceCheckInScan scan = requireActiveScan(requestId, request.deviceId());
        requireUploadValidated(scan);
        if (request.stage().isTerminal() && request.error() == null) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        }
        advance(scan, request.stage(), null, request.error());
    }

    @Transactional
    public void submitEmbedding(UUID requestId, FaceCheckInScanDTO.EmbeddingRequest request) {
        FaceCheckInScan scan = requireActiveScan(requestId, request.deviceId());
        requireUploadValidated(scan);
        validateEmbedding(request.embedding());
        advance(scan, FaceCheckInStage.EMBEDDING_READY, null, null);
        try {
            FaceIdentificationService.IdentifiedPerson identified =
                    faceIdentificationService.identify(null, request.embedding());
            advance(scan, FaceCheckInStage.MATCH_FOUND, null, null);
            LocalDateTime requestedAt = LocalDateTime.ofInstant(scan.getCreatedAt(), ZoneId.systemDefault());
            attendanceCommandService.enqueueFaceCheckIn(new FaceCheckInCommandMessage(
                    requestId,
                    identified.personId(),
                    identified.confidence(),
                    requestedAt));
        } catch (ApiException exception) {
            FaceCheckInResponse.ErrorSummary error = error(exception);
            advance(scan, FaceCheckInStage.REJECTED, null, error);
        }
    }

    @Transactional
    public void recordJavaProgress(UUID requestId, FaceCheckInStage stage) {
        if (stage != FaceCheckInStage.ELIGIBILITY_CHECKED && stage != FaceCheckInStage.ATTENDANCE_SAVED) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        }
        FaceCheckInScan scan = requireActiveScan(requestId, null);
        advance(scan, stage, null, null);
    }

    @Transactional
    public void complete(UUID requestId, FaceCheckInStage stage, FaceCheckInResponse result,
                         FaceCheckInResponse.ErrorSummary error) {
        if (!stage.isTerminal()) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        }
        FaceCheckInScan scan = findScanForUpdate(requestId);
        if (scan.getStage().isTerminal()) {
            return;
        }
        advance(scan, stage, result, error);
    }

    @Transactional(readOnly = true)
    public SseEmitter stream(UUID requestId, long lastEventId) {
        FaceCheckInScan scan = findScan(requestId);
        UUID currentUserId = accessContextResolver.current().userId();
        if (!scan.getUserId().equals(currentUserId)) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_DEVICE_MISMATCH);
        }
        long timeoutMillis = Math.max(1_000L,
                Duration.between(Instant.now(), scan.getExpiresAt().plusSeconds(5)).toMillis());
        SseEmitter emitter = eventStream.subscribe(requestId, timeoutMillis);
        List<FaceCheckInEvent> replay =
                eventRepository.findByRequestIdAndEventIdGreaterThanOrderByEventId(requestId, lastEventId);
        try {
            for (FaceCheckInEvent event : replay) {
                eventStream.send(emitter, toResponse(scan, event));
            }
            if (!replay.isEmpty() && replay.getLast().getStage().isTerminal()) {
                emitter.complete();
            }
        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @Scheduled(fixedDelay = 1_000L)
    @Transactional
    public void expireOverdue() {
        for (UUID requestId : expiryRegistry.pollDue()) {
            FaceCheckInScan scan = scanRepository.findByIdForUpdate(requestId).orElse(null);
            if (scan == null || scan.getStage().isTerminal()) continue;
            if (scan.getExpiresAt().isAfter(Instant.now())) {
                afterCommit(() -> expiryRegistry.track(scan.getRequestId(), scan.getExpiresAt()));
                continue;
            }
            FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                    TrainingErrorCode.FACE_CHECK_IN_EXPIRED.code(),
                    TrainingErrorCode.FACE_CHECK_IN_EXPIRED.title(),
                    TrainingErrorCode.FACE_CHECK_IN_EXPIRED.defaultDetail());
            advance(scan, FaceCheckInStage.EXPIRED, null, error);
        }
    }

    /** Rehydrates this instance's local wake-up queue once after application startup. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void restoreOpenScanExpiries() {
        scanRepository.findByStageNotIn(TERMINAL_STAGES)
                .forEach(scan -> expiryRegistry.track(scan.getRequestId(), scan.getExpiresAt()));
    }

    private FaceCheckInScan requireActiveScan(UUID requestId, String deviceId) {
        FaceCheckInScan scan = findScanForUpdate(requestId);
        if (deviceId != null && !scan.getDeviceId().equals(deviceId.trim())) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_DEVICE_MISMATCH);
        }
        if (scan.getStage().isTerminal()) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        }
        if (!scan.getExpiresAt().isAfter(Instant.now())) {
            FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                    TrainingErrorCode.FACE_CHECK_IN_EXPIRED.code(),
                    TrainingErrorCode.FACE_CHECK_IN_EXPIRED.title(),
                    TrainingErrorCode.FACE_CHECK_IN_EXPIRED.defaultDetail());
            advance(scan, FaceCheckInStage.EXPIRED, null, error);
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_EXPIRED);
        }
        return scan;
    }

    private FaceCheckInScan findScan(UUID requestId) {
        return scanRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.FACE_CHECK_IN_SCAN_NOT_FOUND));
    }

    private FaceCheckInScan findScanForUpdate(UUID requestId) {
        return scanRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.FACE_CHECK_IN_SCAN_NOT_FOUND));
    }

    private void requireUploadValidated(FaceCheckInScan scan) {
        if (scan.getTicketUsedAt() == null) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_UPLOAD_TICKET_INVALID);
        }
    }

    private void validateEmbedding(List<Float> embedding) {
        if (embedding == null || embedding.size() != 512
                || embedding.stream().anyMatch(value -> value == null || !Float.isFinite(value))) {
            throw new ApiException(CoreErrorCode.INVALID_EMBEDDING);
        }
    }

    private void advance(FaceCheckInScan scan, FaceCheckInStage stage, Object result,
                         FaceCheckInResponse.ErrorSummary error) {
        if (eventRepository.existsByRequestIdAndStage(scan.getRequestId(), stage)) {
            return;
        }
        if (scan.getStage().isTerminal() || (!stage.isTerminal() && stage.ordinal() < scan.getStage().ordinal())) {
            throw new ApiException(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        }
        scan.setStage(stage);
        scan.setUpdatedAt(Instant.now());
        appendEvent(scan, stage, result, error);
        if (stage.isTerminal()) afterCommit(() -> expiryRegistry.untrack(scan.getRequestId()));
    }

    private void appendEvent(FaceCheckInScan scan, FaceCheckInStage stage, Object result,
                             FaceCheckInResponse.ErrorSummary error) {
        FaceCheckInEvent event = eventRepository.saveAndFlush(FaceCheckInEvent.builder()
                .requestId(scan.getRequestId())
                .stage(stage)
                .resultPayload(writeJson(result))
                .errorCode(error == null ? null : error.code())
                .errorTitle(error == null ? null : error.title())
                .errorDetail(error == null ? null : error.detail())
                .occurredAt(Instant.now())
                .build());
        FaceCheckInScanDTO.EventResponse response = toResponse(scan, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { eventStream.publish(response); }
            });
        } else {
            eventStream.publish(response);
        }
    }

    private FaceCheckInScanDTO.EventResponse toResponse(FaceCheckInScan scan, FaceCheckInEvent event) {
        FaceCheckInResponse.ErrorSummary error = event.getErrorCode() == null ? null
                : new FaceCheckInResponse.ErrorSummary(
                        event.getErrorCode(), event.getErrorTitle(), event.getErrorDetail());
        return new FaceCheckInScanDTO.EventResponse(
                event.getEventId(),
                event.getRequestId(),
                scan.getDeviceId(),
                event.getStage(),
                event.getOccurredAt(),
                readResult(event.getResultPayload()),
                error);
    }

    private String newUploadTicket() {
        byte[] bytes = new byte[Math.max(24, uploadTicketBytes)];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize face check-in result", exception);
        }
    }

    private FaceCheckInResponse readResult(String value) {
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, FaceCheckInResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize face check-in result", exception);
        }
    }

    private FaceCheckInResponse.ErrorSummary error(ApiException exception) {
        return new FaceCheckInResponse.ErrorSummary(
                exception.getErrorCode().code(),
                exception.getErrorCode().title(),
                exception.responseDetail());
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }
}
