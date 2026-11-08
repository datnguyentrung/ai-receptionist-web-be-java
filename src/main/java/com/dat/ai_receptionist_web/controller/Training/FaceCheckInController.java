package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.scan.FaceCheckInScanDTO;
import com.dat.ai_receptionist_web.service.Training.scan.FaceCheckInScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training/face-check-in-scans")
@RequiredArgsConstructor
public class FaceCheckInController {
    private final FaceCheckInScanService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_CREATE.getCode())" +
                    " or hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_TIMESHEET_CREATE.getCode())")
    public FaceCheckInScanDTO.RegisterResponse register(
            @Valid @RequestBody FaceCheckInScanDTO.RegisterRequest request) {
        return service.register(request);
    }

    @GetMapping(value = "/{requestId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_READ.getCode())" +
                    " or hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COACH_TIMESHEET_READ.getCode())")
    public SseEmitter events(
            @PathVariable UUID requestId,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
        return service.stream(requestId, parseLastEventId(lastEventId));
    }

    private long parseLastEventId(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
