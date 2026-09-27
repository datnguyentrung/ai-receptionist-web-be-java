package com.dat.ai_receptionist_web.dto.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandStatus;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public final class AttendanceCommandDTO {
    private AttendanceCommandDTO() {
    }

    public record Response(
            UUID requestId,
            AttendanceCommandType type,
            AttendanceCommandStatus status,
            Object result,
            FaceCheckInResponse.ErrorSummary error,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime completedAt
    ) {
    }

    public record Receipt(
            UUID requestId,
            AttendanceCommandType type,
            AttendanceCommandStatus status,
            String message
    ) {
    }
}
