package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.dto.Training.command.AttendanceCommandDTO;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.dto.Training.command.SessionAttendanceCommandMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceCommandService {
    private final AttendanceCommandStore store;
    private final AttendanceCommandGateway gateway;

    public AttendanceCommandDTO.Receipt enqueueFaceCheckIn(FaceCheckInCommandMessage message) {
        store.createQueued(message.requestId(), AttendanceCommandType.FACE_CHECK_IN, message);
        afterCommit(() -> gateway.enqueueFaceCheckIn(message));
        return receipt(message.requestId(), AttendanceCommandType.FACE_CHECK_IN);
    }

    public AttendanceCommandDTO.Receipt enqueueSessionAttendance(SessionAttendanceCommandMessage message) {
        store.createQueued(message.requestId(), message.type(), message.payloadJson());
        afterCommit(() -> gateway.enqueueSessionAttendance(message));
        return receipt(message.requestId(), message.type());
    }

    public AttendanceCommandDTO.Response get(UUID requestId) {
        return store.get(requestId);
    }

    private AttendanceCommandDTO.Receipt receipt(UUID requestId, AttendanceCommandType type) {
        return new AttendanceCommandDTO.Receipt(requestId, type,
                com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandStatus.QUEUED,
                "Attendance command queued");
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
