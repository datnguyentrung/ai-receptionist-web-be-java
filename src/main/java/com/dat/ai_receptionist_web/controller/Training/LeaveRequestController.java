package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.LeaveRequestDTO;
import com.dat.ai_receptionist_web.service.Training.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAVE_REQUEST_READ.getCode())")
    public PageResponse<LeaveRequestDTO.SimpleResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAVE_REQUEST_READ.getCode())")
    public LeaveRequestDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAVE_REQUEST_CREATE.getCode())")
    public LeaveRequestDTO.Response create(@Valid @RequestBody LeaveRequestDTO.CreateRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAVE_REQUEST_UPDATE.getCode())")
        public LeaveRequestDTO.Response approve(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveRequestDTO.ReviewCommand command) {
        return service.approve(id, command == null ? null : command.reviewNote());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAVE_REQUEST_UPDATE.getCode())")
        public LeaveRequestDTO.Response reject(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveRequestDTO.ReviewCommand command) {
        return service.reject(id, command == null ? null : command.reviewNote());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAVE_REQUEST_UPDATE.getCode())")
    public LeaveRequestDTO.Response cancel(@PathVariable UUID id) {
        return service.cancel(id);
    }
}
