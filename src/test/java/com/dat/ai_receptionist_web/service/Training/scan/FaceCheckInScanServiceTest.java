package com.dat.ai_receptionist_web.service.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInEvent;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInScan;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.dto.Training.scan.FaceCheckInScanDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.scan.FaceCheckInEventRepository;
import com.dat.ai_receptionist_web.repository.Training.scan.FaceCheckInScanRepository;
import com.dat.ai_receptionist_web.service.Security.access.AccessContext;
import com.dat.ai_receptionist_web.service.Security.access.CurrentAccessContextResolver;
import com.dat.ai_receptionist_web.service.Training.FaceIdentificationService;
import com.dat.ai_receptionist_web.service.Training.command.AttendanceCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FaceCheckInScanServiceTest {
    private final FaceCheckInScanRepository scans = mock(FaceCheckInScanRepository.class);
    private final FaceCheckInEventRepository events = mock(FaceCheckInEventRepository.class);
    private final CurrentAccessContextResolver access = mock(CurrentAccessContextResolver.class);
    private final FaceIdentificationService identification = mock(FaceIdentificationService.class);
    private final AttendanceCommandService commands = mock(AttendanceCommandService.class);
    private final FaceCheckInEventStream stream = mock(FaceCheckInEventStream.class);
    private final FaceCheckInExpiryRegistry expiryRegistry = new FaceCheckInExpiryRegistry();
    private final FaceCheckInScanService service = new FaceCheckInScanService(
            scans, events, access, identification, commands, stream, expiryRegistry, new ObjectMapper());

    @Test
    void registrationTicketIsBoundToRequestAndCanBeValidatedIdempotently() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(access.current()).thenReturn(new AccessContext(userId, null, null, null, Set.of(), Set.of()));
        when(events.saveAndFlush(any())).thenAnswer(invocation -> {
            FaceCheckInEvent value = invocation.getArgument(0);
            return FaceCheckInEvent.builder()
                    .eventId(1L)
                    .requestId(value.getRequestId())
                    .stage(value.getStage())
                    .occurredAt(value.getOccurredAt())
                    .build();
        });
        ReflectionTestUtils.setField(service, "expireAfter", Duration.ofSeconds(30));
        ReflectionTestUtils.setField(service, "uploadTicketBytes", 32);

        FaceCheckInScanDTO.RegisterResponse response = service.register(
                new FaceCheckInScanDTO.RegisterRequest(requestId, "kiosk-01"));

        org.mockito.ArgumentCaptor<FaceCheckInScan> captor =
                org.mockito.ArgumentCaptor.forClass(FaceCheckInScan.class);
        verify(scans).save(captor.capture());
        FaceCheckInScan saved = captor.getValue();
        when(scans.findByIdForUpdate(requestId)).thenReturn(Optional.of(saved));
        FaceCheckInScanDTO.UploadValidationRequest validation =
                new FaceCheckInScanDTO.UploadValidationRequest("kiosk-01", response.uploadTicket());
        service.validateUpload(requestId, validation);
        service.validateUpload(requestId, validation);

        assertThat(saved.getTicketUsedAt()).isNotNull();
        assertThat(response.expiresAt()).isAfter(saved.getCreatedAt());
    }

    @Test
    void fogCannotPublishJavaOwnedStage() {
        UUID requestId = UUID.randomUUID();

        assertThatThrownBy(() -> service.recordFogEvent(requestId,
                new FaceCheckInScanDTO.EventRequest(
                        "kiosk-01", FaceCheckInStage.MATCH_FOUND, null)))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(TrainingErrorCode.FACE_CHECK_IN_STAGE_INVALID);
        verify(scans, never()).findByIdForUpdate(requestId);
    }

    @Test
    void idleSchedulerNeverQueriesTheDatabase() {
        service.expireOverdue();

        verify(scans, never()).findByIdForUpdate(any());
    }

    @Test
    void dueScanIsLoadedAndExpiredExactlyOnce() {
        UUID requestId = UUID.randomUUID();
        FaceCheckInScan scan = FaceCheckInScan.builder()
                .requestId(requestId)
                .deviceId("kiosk-01")
                .stage(FaceCheckInStage.READY)
                .expiresAt(Instant.now().minusSeconds(1))
                .createdAt(Instant.now().minusSeconds(31))
                .updatedAt(Instant.now().minusSeconds(31))
                .build();
        expiryRegistry.track(requestId, scan.getExpiresAt());
        when(scans.findByIdForUpdate(requestId)).thenReturn(Optional.of(scan));
        when(events.saveAndFlush(any())).thenAnswer(invocation -> {
            FaceCheckInEvent value = invocation.getArgument(0);
            return FaceCheckInEvent.builder().eventId(1L).requestId(requestId)
                    .stage(value.getStage()).occurredAt(value.getOccurredAt()).build();
        });

        service.expireOverdue();
        service.expireOverdue();

        assertThat(scan.getStage()).isEqualTo(FaceCheckInStage.EXPIRED);
        verify(scans).findByIdForUpdate(requestId);
        verify(events).saveAndFlush(any());
    }

    @Test
    void startupRestoresOnlyOpenScansIntoTheLocalQueue() {
        UUID requestId = UUID.randomUUID();
        FaceCheckInScan scan = FaceCheckInScan.builder()
                .requestId(requestId).deviceId("kiosk-01").stage(FaceCheckInStage.READY)
                .expiresAt(Instant.now().minusSeconds(1)).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(scans.findByStageNotIn(any())).thenReturn(List.of(scan));
        when(scans.findByIdForUpdate(requestId)).thenReturn(Optional.of(scan));
        when(events.saveAndFlush(any())).thenAnswer(invocation -> {
            FaceCheckInEvent value = invocation.getArgument(0);
            return FaceCheckInEvent.builder().eventId(1L).requestId(requestId)
                    .stage(value.getStage()).occurredAt(value.getOccurredAt()).build();
        });

        service.restoreOpenScanExpiries();
        service.expireOverdue();

        verify(scans).findByStageNotIn(any());
        verify(scans).findByIdForUpdate(requestId);
    }
}
