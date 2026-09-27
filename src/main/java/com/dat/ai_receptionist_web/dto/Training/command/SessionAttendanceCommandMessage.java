package com.dat.ai_receptionist_web.dto.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;

import java.util.UUID;

public record SessionAttendanceCommandMessage(
        UUID requestId,
        AttendanceCommandType type,
        String payloadJson
) {
}
