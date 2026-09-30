package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.domain.Training.command.AttendanceCommandType;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceDTO;
import com.dat.ai_receptionist_web.dto.Training.SessionAttendanceFilter;
import com.dat.ai_receptionist_web.dto.Training.command.AttendanceCommandDTO;
import com.dat.ai_receptionist_web.service.Training.SessionAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session-attendances")
@RequiredArgsConstructor
public class SessionAttendanceController {
    private final SessionAttendanceService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_READ.getCode())")
    public PageResponse<SessionAttendanceDTO.SimpleResponse> list(
            @Valid @ModelAttribute SessionAttendanceFilter filter,
            Pageable pageable
    ) {
        return service.list(filter, pageable);
    }

    @GetMapping("/with-stats")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_READ.getCode())")
    public SessionAttendanceDTO.AttendanceListResponse listWithStats(
            @Valid @ModelAttribute SessionAttendanceFilter filter,
            Pageable pageable
    ) {
        return service.listWithStats(filter, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_READ.getCode())")
    public SessionAttendanceDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_CREATE.getCode())")
    public SessionAttendanceDTO.Response create(@Valid @RequestBody SessionAttendanceDTO.CreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_UPDATE.getCode())")
    public SessionAttendanceDTO.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody SessionAttendanceDTO.UpdateRequest request
    ) {
        return service.update(id, request);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_UPDATE.getCode())")
    public AttendanceCommandDTO.Receipt updateBatch(
            @Valid @RequestBody SessionAttendanceDTO.BatchUpdateRequest request
    ) {
        return service.enqueueCommand(AttendanceCommandType.SESSION_ATTENDANCE_BATCH_UPDATE, request);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_UPDATE.getCode())")
    public AttendanceCommandDTO.Receipt updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SessionAttendanceDTO.UpdateStatusRequest request
    ) {
        return service.enqueueCommand(
                AttendanceCommandType.SESSION_ATTENDANCE_STATUS_UPDATE,
                new SessionAttendanceService.IdPayload<>(id, request)
        );
    }

    @PatchMapping("/{id}/evaluation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_UPDATE.getCode())")
    public AttendanceCommandDTO.Receipt updateEvaluation(
            @PathVariable UUID id,
            @Valid @RequestBody SessionAttendanceDTO.UpdateEvaluationRequest request
    ) {
        return service.enqueueCommand(
                AttendanceCommandType.SESSION_ATTENDANCE_EVALUATION_UPDATE,
                new SessionAttendanceService.IdPayload<>(id, request)
        );
    }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_CREATE.getCode())")
    public AttendanceCommandDTO.Receipt createManual(
            @Valid @RequestBody SessionAttendanceDTO.ManualLogRequest request
    ) {
        return service.enqueueCommand(AttendanceCommandType.SESSION_ATTENDANCE_MANUAL_CREATE, request);
    }

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_CREATE.getCode())")
    public AttendanceCommandDTO.Receipt quickCheckIn(
            @Valid @RequestBody SessionAttendanceDTO.QuickCheckInRequest request
    ) {
        return service.enqueueCommand(AttendanceCommandType.SESSION_ATTENDANCE_QUICK_CHECK_IN, request);
    }

    @PostMapping("/batch-init")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_CREATE.getCode())")
    public AttendanceCommandDTO.Receipt batchInit(
            @Valid @RequestBody SessionAttendanceDTO.BatchInitRequest request
    ) {
        return service.enqueueCommand(AttendanceCommandType.SESSION_ATTENDANCE_BATCH_INIT, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).SESSION_ATTENDANCE_DELETE.getCode())")
    public AttendanceCommandDTO.Receipt bulkDelete(
            @Valid @RequestBody SessionAttendanceDTO.BulkDeleteRequest request
    ) {
        return service.enqueueCommand(AttendanceCommandType.SESSION_ATTENDANCE_BULK_DELETE, request);
    }
}
