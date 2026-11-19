package com.dat.ai_receptionist_web.controller.Crm;

import com.dat.ai_receptionist_web.dto.Crm.LeadDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Crm.LeadService;
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
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {
    private final LeadService service;

    /**
     * Lấy danh sách khách hàng tiềm năng theo phân trang.
     */
    @GetMapping
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAD_READ.getCode())")
    public PageResponse<LeadDTO.SimpleResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    /**
     * Lấy chi tiết một khách hàng tiềm năng.
     */
    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAD_READ.getCode())")
    public LeadDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    /**
     * Tạo mới một khách hàng tiềm năng.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAD_CREATE.getCode())")
    public LeadDTO.Response create(@Valid @RequestBody LeadDTO.CreateRequest request) {
        return service.create(request);
    }

    /**
     * Cập nhật toàn bộ thông tin của một khách hàng tiềm năng.
     */
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAD_UPDATE.getCode())")
    public LeadDTO.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody LeadDTO.UpdateRequest request
    ) {
        return service.update(
                id,
                request
        );
    }

    /**
     * Xóa một khách hàng tiềm năng chưa có lịch học thử.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(
            "hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).LEAD_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
