package com.dat.ai_receptionist_web.dto.Training.scan;

import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FaceCheckInScanEventResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void rejectedEventSerializesRecognizedPersonInsideResult() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                "FACE_CHECK_IN_NO_ACTIVE_CONTEXT",
                "No active face check-in context",
                "No active attendance context was found for this person"
        );
        FaceCheckInResponse result = new FaceCheckInResponse(
                FaceCheckInResponse.Status.REJECTED,
                null,
                new FaceCheckInResponse.PersonSummary(
                        personId,
                        "Nguyen Van A",
                        "VQ_001",
                        "https://example.test/face.jpg"
                ),
                null,
                null,
                null,
                null,
                requestId,
                0.91f,
                null,
                error.detail(),
                error
        );
        FaceCheckInScanDTO.EventResponse event = new FaceCheckInScanDTO.EventResponse(
                46L,
                requestId,
                "kiosk-01",
                FaceCheckInStage.REJECTED,
                Instant.parse("2026-11-07T15:58:36Z"),
                result,
                error
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(event));

        assertThat(json.path("result").isObject()).isTrue();
        assertThat(json.path("result").path("person").path("personId").asText())
                .isEqualTo(personId.toString());
        assertThat(json.path("result").path("person").path("fullName").asText())
                .isEqualTo("Nguyen Van A");
        assertThat(json.path("result").path("person").path("personCode").asText())
                .isEqualTo("VQ_001");
        assertThat(json.path("result").path("error").path("code").asText())
                .isEqualTo("FACE_CHECK_IN_NO_ACTIVE_CONTEXT");
    }
}
