package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.Training.EnrollmentScheduleChangeRequestDTO;
import com.dat.ai_receptionist_web.service.Training.EnrollmentScheduleChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollment-schedule-change-requests")
@RequiredArgsConstructor
public class EnrollmentScheduleChangeRequestController {
    private final EnrollmentScheduleChangeRequestService service;

    @GetMapping("/options")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_READ.getCode())")
    public List<EnrollmentScheduleChangeRequestDTO.ScheduleOption> options(
            @RequestParam UUID currentScheduleId,
            @RequestParam LocalDate effectiveFrom) {
        return service.options(currentScheduleId, effectiveFrom);
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public EnrollmentScheduleChangeRequestDTO.Response create(
            @Valid @RequestBody EnrollmentScheduleChangeRequestDTO.CreateRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public EnrollmentScheduleChangeRequestDTO.Response approve(@PathVariable UUID id,
            @RequestBody(required = false) EnrollmentScheduleChangeRequestDTO.ReviewRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).STUDENT_ENROLLMENT_UPDATE.getCode())")
    public EnrollmentScheduleChangeRequestDTO.Response reject(@PathVariable UUID id,
            @RequestBody(required = false) EnrollmentScheduleChangeRequestDTO.ReviewRequest request) {
        return service.reject(id, request);
    }
}
