package com.dat.ai_receptionist_web.controller.Core;

import com.dat.ai_receptionist_web.service.Core.TenantProvisioningService;
import com.dat.ai_receptionist_web.service.Security.PlatformOwnerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {
    private final TenantProvisioningService provisioningService;
    private final PlatformOwnerService platformOwnerService;

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_CREATE')")
    public TenantProvisioningService.TenantProvisionResult create(@Valid @RequestBody CreateTenantRequest request) {
        platformOwnerService.requirePlatformOwner();
        boolean hasSystemAdminInTenant = com.dat.ai_receptionist_web.util.SecurityUtil.getJwt()
                .map(jwt -> jwt.getClaimAsStringList("roles"))
                .map(roles -> roles.contains(com.dat.ai_receptionist_web.service.Security.PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE))
                .orElse(false);
        if (!hasSystemAdminInTenant) {
            throw new com.dat.ai_receptionist_web.error.ApiException(
                    com.dat.ai_receptionist_web.error.code.SecurityErrorCode.ACCESS_DENIED,
                    "SYSTEM_ADMIN role is required in the active tenant");
        }
        return provisioningService.provisionTenant(request.code(), request.name(), request.initialAdminUserId());
    }

    public record CreateTenantRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull UUID initialAdminUserId
    ) {
    }
}
