package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.config.RabbitMQConfig;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.dto.Training.command.FaceCheckInCommandMessage;
import com.dat.ai_receptionist_web.dto.Training.command.SessionAttendanceCommandMessage;
import com.dat.ai_receptionist_web.domain.Training.scan.FaceCheckInStage;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.service.Training.FaceCheckInService;
import com.dat.ai_receptionist_web.service.Training.scan.FaceCheckInScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceCommandWorker {
    private final AttendanceCommandStore store;
    private final FaceCheckInService faceCheckInService;
    private final SessionAttendanceCommandExecutor sessionAttendanceCommandExecutor;
    private final FaceCheckInScanService faceCheckInScanService;

    @RabbitListener(
            queues = RabbitMQConfig.FACE_CHECK_IN_COMMAND_QUEUE,
            containerFactory = "rabbitListenerContainerFactory",
            errorHandler = "rabbitMQErrorHandler"
    )
    public void handleFaceCheckIn(FaceCheckInCommandMessage message) {
        log.info("Processing face check-in command requestId={} personId={}",
                message.requestId(), message.personId());
        store.markProcessing(message.requestId());
        try {
            FaceCheckInResponse result = faceCheckInService.processQueuedCheckIn(
                    message.requestId(),
                    message.personId(),
                    message.confidence(),
                    message.requestedAt()
            );
            if (result.status() == FaceCheckInResponse.Status.REJECTED) {
                store.markRejected(message.requestId(), result, result.error());
                faceCheckInScanService.complete(
                        message.requestId(), FaceCheckInStage.REJECTED, result, result.error());
                return;
            }
            if (result.status() == FaceCheckInResponse.Status.FAILED) {
                store.markFailed(message.requestId(), result.error());
                faceCheckInScanService.complete(
                        message.requestId(), FaceCheckInStage.FAILED, result, result.error());
                return;
            }
            store.markSucceeded(message.requestId(), result);
            faceCheckInScanService.complete(
                    message.requestId(), FaceCheckInStage.SUCCEEDED, result, null);
        } catch (ApiException exception) {
            FaceCheckInResponse.ErrorSummary error = new FaceCheckInResponse.ErrorSummary(
                    exception.getErrorCode().code(),
                    exception.getErrorCode().title(),
                    exception.responseDetail()
            );
            store.markFailed(message.requestId(), error);
            faceCheckInScanService.complete(message.requestId(), FaceCheckInStage.FAILED, null, error);
        }
    }

    @RabbitListener(
            queues = RabbitMQConfig.SESSION_ATTENDANCE_COMMAND_QUEUE,
            containerFactory = "rabbitListenerContainerFactory",
            errorHandler = "rabbitMQErrorHandler"
    )
    public void handleSessionAttendance(SessionAttendanceCommandMessage message) {
        log.info("Processing session attendance command requestId={} type={}",
                message.requestId(), message.type());
        store.markProcessing(message.requestId());
        try {
            Object result = sessionAttendanceCommandExecutor.execute(message.requestId(), message.type());
            store.markSucceeded(message.requestId(), result);
        } catch (ApiException exception) {
            store.markFailed(message.requestId(), new FaceCheckInResponse.ErrorSummary(
                    exception.getErrorCode().code(),
                    exception.getErrorCode().title(),
                    exception.responseDetail()
            ));
        }
    }
}
