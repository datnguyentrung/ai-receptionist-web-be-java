package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Core.Tenant;
import com.dat.ai_receptionist_web.domain.Core.TenantMembership;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserRole;
import com.dat.ai_receptionist_web.enums.Core.TenantMembershipStatus;
import com.dat.ai_receptionist_web.enums.Core.TenantStatus;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.repository.Core.TenantMembershipRepository;
import com.dat.ai_receptionist_web.repository.Core.TenantRepository;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRoleRepository;
import com.dat.ai_receptionist_web.service.Security.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Luồng quản trị nền tảng (Platform Provisioning):
 * tạo trung tâm mới, khởi tạo bộ role chuẩn trong tenant và gán quản trị viên đầu tiên.
 */
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionService rolePermissionService;
    private final com.dat.ai_receptionist_web.service.Security.PlatformOwnerService platformOwnerService;

    @Transactional
    public TenantProvisionResult provisionTenant(String code, String name, UUID initialAdminUserId) {
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (tenantRepository.existsByCode(normalizedCode)) {
            throw new ApiException(SecurityErrorCode.TENANT_ALREADY_EXISTS);
        }

        User adminUser = userRepository.findById(initialAdminUserId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .code(normalizedCode)
                .name(name.trim())
                .status(TenantStatus.ACTIVE)
                .build());

        Map<String, Set<String>> templatePermissionsByRole = TenantContext.runWithTenant(
                TenantContext.DEFAULT_TENANT_ID,
                () -> {
                    Map<String, Set<String>> map = new LinkedHashMap<>();
                    List<Role> defaultRoles = roleRepository.findAllByTenantId(
                            TenantContext.DEFAULT_TENANT_ID,
                            org.springframework.data.domain.Pageable.unpaged()
                    ).getContent();
                    for (Role template : defaultRoles) {
                        var perms = roleRepository.findPermissionsByTenantIdAndRoleCodeIn(
                                TenantContext.DEFAULT_TENANT_ID, Set.of(template.getCode()));
                        Set<String> permCodes = perms.stream()
                                .map(RoleRepository.RolePermissionRow::getPermissionCode)
                                .collect(java.util.stream.Collectors.toSet());
                        if (!permCodes.isEmpty()) {
                            map.put(template.getCode(), permCodes);
                        }
                    }
                    return map;
                }
        );

        UUID tenantId = tenant.getTenantId();
        TenantContext.runWithTenant(tenantId, () -> {
            for (SystemRoleDefinition definition : SystemRoleDefinition.values()) {
                if (!roleRepository.existsByTenantIdAndCode(tenantId, definition.getCode())) {
                    roleRepository.save(Role.builder()
                            .tenantId(tenantId)
                            .code(definition.getCode())
                            .name(definition.getName())
                            .description(definition.getDescription())
                            .permissionVersion(0L)
                            .build());
                }
            }

            templatePermissionsByRole.forEach((roleCode, permCodes) -> {
                if (roleRepository.existsByTenantIdAndCode(tenantId, roleCode)) {
                    rolePermissionService.replaceInternal(roleCode, permCodes);
                }
            });

            tenantMembershipRepository.save(TenantMembership.builder()
                    .tenantId(tenantId)
                    .user(adminUser)
                    .status(TenantMembershipStatus.ACTIVE)
                    .build());

            String adminRoleCode = SystemRoleDefinition.SUPER_ADMIN.getCode();
            Role adminRole = roleRepository.findByTenantIdAndCode(tenantId, adminRoleCode)
                    .orElseThrow(() -> new ApiException(SecurityErrorCode.ROLE_NOT_FOUND));
            userRoleRepository.save(new UserRole(
                    new UserRole.Key(tenantId, adminUser.getUserId(), adminRole.getCode()),
                    adminUser,
                    adminRole
            ));
            userRepository.incrementAuthorizationVersion(adminUser.getUserId());

            platformOwnerService.findPlatformOwnerUserId().ifPresent(ownerUserId -> {
                User ownerUser = userRepository.findById(ownerUserId)
                        .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
                if (!ownerUserId.equals(adminUser.getUserId())) {
                    tenantMembershipRepository.save(TenantMembership.builder()
                            .tenantId(tenantId)
                            .user(ownerUser)
                            .status(TenantMembershipStatus.ACTIVE)
                            .build());
                }
                Role systemAdminRole = roleRepository.findByTenantIdAndCode(
                                tenantId, SystemRoleDefinition.SYSTEM_ADMIN.getCode())
                        .orElseThrow(() -> new ApiException(SecurityErrorCode.ROLE_NOT_FOUND));
                userRoleRepository.save(new UserRole(
                        new UserRole.Key(tenantId, ownerUserId, systemAdminRole.getCode()),
                        ownerUser,
                        systemAdminRole
                ));
                userRepository.incrementAuthorizationVersion(ownerUserId);
            });
        });

        return new TenantProvisionResult(tenant.getTenantId(), tenant.getCode(), tenant.getName(), tenant.getStatus());
    }

    public record TenantProvisionResult(UUID tenantId, String code, String name, TenantStatus status) {
    }
}
