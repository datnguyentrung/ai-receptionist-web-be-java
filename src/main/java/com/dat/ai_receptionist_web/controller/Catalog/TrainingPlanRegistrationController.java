package com.dat.ai_receptionist_web.controller.Catalog;

import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanRegistrationDTO;
import com.dat.ai_receptionist_web.service.Catalog.TrainingPlanRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/training-plans")
@RequiredArgsConstructor
public class TrainingPlanRegistrationController {
    private final TrainingPlanRegistrationService service;

    @GetMapping("/registration-catalog")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PURCHASE_CREATE.getCode())")
    public List<TrainingPlanRegistrationDTO.CatalogItem> catalog(@RequestParam(required = false) UUID studentPersonId) {
        return service.catalog();
    }

    @GetMapping("/{planId}/registration-options")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_PURCHASE_CREATE.getCode())")
    public TrainingPlanRegistrationDTO.RegistrationOptions options(@PathVariable UUID planId,
                                                                     @RequestParam LocalDate startDate) {
        return service.options(planId, startDate);
    }
}
