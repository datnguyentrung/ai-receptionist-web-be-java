package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.service.Training.SessionAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionAttendanceCommandExecutor {
    private final AttendanceCommandStore store;
    private final SessionAttendanceService sessionAttendanceService;

    public Object execute(UUID requestId, AttendanceCommandType type) {
        String payload = store.getEntity(requestId).getPayload();
        return sessionAttendanceService.executeQueuedCommand(type, payload);
    }
}
