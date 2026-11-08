package com.dat.ai_receptionist_web.domain.Training.scan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "face_check_in_event", schema = "training")
public class FaceCheckInEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private FaceCheckInStage stage;

    @Column(name = "result_payload", columnDefinition = "text")
    private String resultPayload;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_title")
    private String errorTitle;

    @Column(name = "error_detail", columnDefinition = "text")
    private String errorDetail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
