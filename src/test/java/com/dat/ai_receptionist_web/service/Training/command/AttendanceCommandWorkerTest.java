package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.dto.Training.command.SessionAttendanceCommandMessage;
import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.service.Training.FaceCheckInService;
import com.dat.ai_receptionist_web.service.Training.scan.FaceCheckInScanService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AttendanceCommandWorkerTest {
    private final AttendanceCommandStore store = mock(AttendanceCommandStore.class);
    private final FaceCheckInService faceCheckInService = mock(FaceCheckInService.class);
    private final SessionAttendanceCommandExecutor executor = mock(SessionAttendanceCommandExecutor.class);
    private final FaceCheckInScanService faceCheckInScanService = mock(FaceCheckInScanService.class);
    private final AttendanceCommandWorker worker = new AttendanceCommandWorker(
            store,
            faceCheckInService,
            executor,
            faceCheckInScanService
    );

    @Test
    void unexpectedFailureMarksCommandFailedInsteadOfLeavingItProcessing() {
        UUID requestId = UUID.randomUUID();
        SessionAttendanceCommandMessage message = new SessionAttendanceCommandMessage(
                requestId,
                AttendanceCommandType.SESSION_ATTENDANCE_EVALUATION_UPDATE,
                "{}"
        );
        when(executor.execute(requestId, message.type()))
                .thenThrow(new IllegalStateException("transaction failed"));

        worker.handleSessionAttendance(message);

        ArgumentCaptor<FaceCheckInResponse.ErrorSummary> error =
                ArgumentCaptor.forClass(FaceCheckInResponse.ErrorSummary.class);
        verify(store).markProcessing(requestId);
        verify(store).markFailed(eq(requestId), error.capture());
        assertThat(error.getValue().code())
                .isEqualTo(GeneralErrorCode.INTERNAL_SERVER_ERROR.code());
    }

    @Test
    void unexpectedFaceCheckInFailureCompletesTheScanInsteadOfDeadLetteringIt() {
        UUID requestId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        FaceCheckInCommandMessage message = new FaceCheckInCommandMessage(
                requestId, personId, 0.91f, java.time.LocalDateTime.now());
        when(faceCheckInService.processQueuedCheckIn(
                requestId, personId, message.confidence(), message.requestedAt()))
                .thenThrow(new IllegalStateException("transaction failed"));

        worker.handleFaceCheckIn(message);

        ArgumentCaptor<FaceCheckInResponse.ErrorSummary> error =
                ArgumentCaptor.forClass(FaceCheckInResponse.ErrorSummary.class);
        verify(store).markProcessing(requestId);
        verify(store).markFailed(eq(requestId), error.capture());
        verify(faceCheckInScanService).complete(
                eq(requestId), eq(FaceCheckInStage.FAILED), isNull(), eq(error.getValue()));
        assertThat(error.getValue().code())
                .isEqualTo(GeneralErrorCode.INTERNAL_SERVER_ERROR.code());
    }
}
