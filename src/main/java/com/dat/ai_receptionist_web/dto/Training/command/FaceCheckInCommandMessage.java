package com.dat.ai_receptionist_web.dto.Training.command;

import java.time.LocalDateTime;
import java.util.UUID;

public record FaceCheckInCommandMessage(
        UUID requestId,
        UUID personId,
        float confidence,
        LocalDateTime requestedAt
) {
}
