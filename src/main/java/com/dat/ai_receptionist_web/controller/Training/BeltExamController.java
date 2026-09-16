package com.dat.ai_receptionist_web.controller.Training;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Training.BeltExamDTO;
import com.dat.ai_receptionist_web.service.Training.BeltExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/belt-exams")
@RequiredArgsConstructor
public class BeltExamController {
    private final BeltExamService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).BELT_EXAM_READ.getCode())")
    public PageResponse<BeltExamDTO.SimpleResponse> list(Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).BELT_EXAM_READ.getCode())")
    public BeltExamDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).BELT_EXAM_CREATE.getCode())")
    public BeltExamDTO.Response create(@Valid @RequestBody BeltExamDTO.CreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).BELT_EXAM_UPDATE.getCode())")
    public BeltExamDTO.Response update(@PathVariable UUID id, @Valid @RequestBody BeltExamDTO.UpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).BELT_EXAM_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
