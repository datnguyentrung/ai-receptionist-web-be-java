package com.dat.ai_receptionist_web.service.Training.command;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommand;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandStatus;
import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.dto.Training.FaceCheckInResponse;
import com.dat.ai_receptionist_web.dto.Training.command.AttendanceCommandDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.repository.Training.command.AttendanceCommandRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceCommandStore {
    private final AttendanceCommandRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AttendanceCommand createQueued(UUID requestId, AttendanceCommandType type, Object payload) {
        LocalDateTime now = LocalDateTime.now();
        AttendanceCommand command = AttendanceCommand.builder()
                .requestId(requestId)
                .commandType(type)
                .status(AttendanceCommandStatus.QUEUED)
                .payload(writeJson(payload))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return repository.save(command);
    }

    @Transactional
    public void markProcessing(UUID requestId) {
        AttendanceCommand command = getEntity(requestId);
        if (command.getStatus() == AttendanceCommandStatus.SUCCEEDED
                || command.getStatus() == AttendanceCommandStatus.REJECTED
                || command.getStatus() == AttendanceCommandStatus.FAILED
                || command.getStatus() == AttendanceCommandStatus.EXPIRED) {
            return;
        }
        command.setStatus(AttendanceCommandStatus.PROCESSING);
        command.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void markSucceeded(UUID requestId, Object result) {
        AttendanceCommand command = getEntity(requestId);
        LocalDateTime now = LocalDateTime.now();
        command.setStatus(AttendanceCommandStatus.SUCCEEDED);
        command.setResult(writeJson(result));
        command.setErrorCode(null);
        command.setErrorTitle(null);
        command.setErrorDetail(null);
        command.setUpdatedAt(now);
        command.setCompletedAt(now);
    }

    @Transactional
    public void markRejected(UUID requestId, Object result, FaceCheckInResponse.ErrorSummary error) {
        AttendanceCommand command = getEntity(requestId);
        LocalDateTime now = LocalDateTime.now();
        command.setStatus(AttendanceCommandStatus.REJECTED);
        command.setResult(writeJson(result));
        command.setErrorCode(error == null ? null : error.code());
        command.setErrorTitle(error == null ? null : error.title());
        command.setErrorDetail(error == null ? null : error.detail());
        command.setUpdatedAt(now);
        command.setCompletedAt(now);
    }

    @Transactional
    public void markFailed(UUID requestId, FaceCheckInResponse.ErrorSummary error) {
        AttendanceCommand command = getEntity(requestId);
        LocalDateTime now = LocalDateTime.now();
        command.setStatus(AttendanceCommandStatus.FAILED);
        command.setErrorCode(error == null ? null : error.code());
        command.setErrorTitle(error == null ? null : error.title());
        command.setErrorDetail(error == null ? null : error.detail());
        command.setUpdatedAt(now);
        command.setCompletedAt(now);
    }

    @Transactional
    public void markExpired(UUID requestId, FaceCheckInResponse.ErrorSummary error) {
        AttendanceCommand command = getEntity(requestId);
        if (command.getStatus() == AttendanceCommandStatus.SUCCEEDED
                || command.getStatus() == AttendanceCommandStatus.REJECTED
                || command.getStatus() == AttendanceCommandStatus.FAILED
                || command.getStatus() == AttendanceCommandStatus.EXPIRED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        command.setStatus(AttendanceCommandStatus.EXPIRED);
        command.setErrorCode(error == null ? null : error.code());
        command.setErrorTitle(error == null ? null : error.title());
        command.setErrorDetail(error == null ? null : error.detail());
        command.setUpdatedAt(now);
        command.setCompletedAt(now);
    }

    @Transactional(readOnly = true)
    public AttendanceCommandDTO.Response get(UUID requestId) {
        AttendanceCommand command = getEntity(requestId);
        Object result = readJson(command.getResult());
        FaceCheckInResponse.ErrorSummary error = command.getErrorCode() == null
                ? null
                : new FaceCheckInResponse.ErrorSummary(
                command.getErrorCode(),
                command.getErrorTitle(),
                command.getErrorDetail()
        );
        return new AttendanceCommandDTO.Response(
                command.getRequestId(),
                command.getCommandType(),
                command.getStatus(),
                result,
                error,
                command.getCreatedAt(),
                command.getUpdatedAt(),
                command.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public AttendanceCommand getEntity(UUID requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.ATTENDANCE_COMMAND_NOT_FOUND));
    }

    public <T> T readPayload(AttendanceCommand command, Class<T> type) {
        return readTypedJson(command.getPayload(), type, "payload");
    }

    public <T> T readResult(AttendanceCommand command, Class<T> type) {
        return readTypedJson(command.getResult(), type, "result");
    }

    private <T> T readTypedJson(String json, Class<T> type, String fieldName) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize attendance command " + fieldName, exception);
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize attendance command payload", exception);
        }
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize attendance command result", exception);
        }
    }
}
