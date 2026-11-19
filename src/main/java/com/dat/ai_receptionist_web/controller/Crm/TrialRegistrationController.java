package com.dat.ai_receptionist_web.controller.Crm;

import com.dat.ai_receptionist_web.dto.Crm.TrialRegistrationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Crm.TrialRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trial-registrations")
@RequiredArgsConstructor
public class TrialRegistrationController {
    private final TrialRegistrationService service;

    /** Lấy danh sách lịch học thử theo phân trang. */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRIAL_REGISTRATION_READ.getCode())")
    public PageResponse<TrialRegistrationDTO.SimpleResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    /** Lấy chi tiết một lịch học thử. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRIAL_REGISTRATION_READ.getCode())")
    public TrialRegistrationDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    /** Tạo mới một lịch học thử cho khách hàng tiềm năng. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRIAL_REGISTRATION_CREATE.getCode())")
    public TrialRegistrationDTO.Response create(
            @Valid @RequestBody TrialRegistrationDTO.CreateRequest request) {
        return service.create(request);
    }

    /** Cập nhật thông tin và trạng thái của một lịch học thử. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRIAL_REGISTRATION_UPDATE.getCode())")
    public TrialRegistrationDTO.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody TrialRegistrationDTO.UpdateRequest request) {
        return service.update(id, request);
    }

    /** Xóa một lịch học thử. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRIAL_REGISTRATION_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
