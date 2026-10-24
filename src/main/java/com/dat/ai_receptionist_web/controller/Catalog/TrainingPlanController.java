package com.dat.ai_receptionist_web.controller.Catalog;

import com.dat.ai_receptionist_web.dto.*;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanDTO;
import com.dat.ai_receptionist_web.enums.Catalog.TrainingPlanStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.service.Catalog.TrainingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/training-plans")
@RequiredArgsConstructor
public class TrainingPlanController {
    private final TrainingPlanService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_READ.getCode())")
    public PageResponse<TrainingPlanDTO.SimpleResponse> list(
            @RequestParam(required = false) TrainingPlanStatus status,
            @RequestParam(required = false) ScheduleLevel level,
            Pageable pageable
    ) {
        return service.list(
                status,
                level,
                pageable
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_READ.getCode())")
    public TrainingPlanDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_CREATE.getCode())")
    public TrainingPlanDTO.Response create(@Valid @RequestBody TrainingPlanDTO.CreateRequest r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_UPDATE.getCode())")
    public TrainingPlanDTO.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody TrainingPlanDTO.UpdateRequest r
    ) {
        return service.update(
                id,
                r
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
