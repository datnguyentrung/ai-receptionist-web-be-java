package com.dat.ai_receptionist_web.controller.Catalog;

import com.dat.ai_receptionist_web.dto.*;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanAccessRuleDTO;
import com.dat.ai_receptionist_web.service.Catalog.TrainingPlanAccessRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/training-plan-access-rules")
@RequiredArgsConstructor
public class TrainingPlanAccessRuleController {
    private final TrainingPlanAccessRuleService service;

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_ACCESS_RULE_READ.getCode())")
    public PageResponse<TrainingPlanAccessRuleDTO.SimpleResponse> list(
            @RequestParam(required = false) UUID trainingPlanId,
            Pageable p
    ) {
        return service.list(
                trainingPlanId,
                p
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_ACCESS_RULE_READ.getCode())")
    public TrainingPlanAccessRuleDTO.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_ACCESS_RULE_CREATE.getCode())")
    public TrainingPlanAccessRuleDTO.Response create(@Valid @RequestBody TrainingPlanAccessRuleDTO.CreateRequest r) {
        return service.create(r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_ACCESS_RULE_UPDATE.getCode())")
    public TrainingPlanAccessRuleDTO.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody TrainingPlanAccessRuleDTO.UpdateRequest r
    ) {
        return service.update(
                id,
                r
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).TRAINING_PLAN_ACCESS_RULE_DELETE.getCode())")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
