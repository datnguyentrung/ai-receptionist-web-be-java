package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Security.RolePermissionMapper;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RolePermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionMapper rolePermissionMapper;
    private final PlatformOwnerService platformOwnerService;

    @Transactional(readOnly = true)
    public PageResponse<RolePermissionDTO.SimpleResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        var page = platformOwnerService.isCurrentUserPlatformOwner()
                ? rolePermissionRepository.findAllDetailedByTenantId(tenantId, pageable)
                : rolePermissionRepository.findAllDetailedByTenantIdAndRoleCodeNot(
                        tenantId, PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE, pageable);
        return PageResponse.of(page, rolePermissionMapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public RolePermissionDTO.ItemResponse get(String roleCode, Integer permissionId) {
        if (!platformOwnerService.isCurrentUserPlatformOwner()
                && PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode)) {
            throw new ApiException(SecurityErrorCode.ROLE_PERMISSION_NOT_FOUND);
        }
        return rolePermissionMapper.toResponse(find(roleCode, permissionId));
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RolePermissionDTO.ItemResponse create(RolePermissionDTO.CreateRequest request) {
        rejectSystemAdminPermissionMutation(request.roleCode());
        UUID tenantId = TenantContext.requireTenantId();
        Role role = roleRepository.findByTenantIdAndCode(tenantId, request.roleCode())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.ROLE_NOT_FOUND));
        Permission permission = permissionRepository.findById(request.permissionId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.PERMISSION_NOT_FOUND));
        if (PermissionDefinition.TENANT_ADMIN_CODES.contains(permission.getCode())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "Tenant admin permissions are reserved for SYSTEM_ADMIN");
        }
        RolePermission entity = new RolePermission(
                new RolePermission.Key(tenantId, role.getCode(), permission.getPermissionId()), role, permission);
        RolePermission saved = rolePermissionRepository.save(entity);
        roleRepository.incrementPermissionVersion(tenantId, role.getCode());
        return rolePermissionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public void delete(String roleCode, Integer permissionId) {
        rejectSystemAdminPermissionMutation(roleCode);
        UUID tenantId = TenantContext.requireTenantId();
        rolePermissionRepository.delete(find(roleCode, permissionId));
        roleRepository.incrementPermissionVersion(tenantId, roleCode);
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RolePermissionDTO.Response replace(String roleCode, Set<String> requestedCodes) {
        rejectSystemAdminPermissionMutation(roleCode);
        if (requestedCodes != null && requestedCodes.stream()
                .filter(Objects::nonNull)
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .anyMatch(PermissionDefinition.TENANT_ADMIN_CODES::contains)) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "Tenant admin permissions are reserved for SYSTEM_ADMIN");
        }
        SyncResult result = replaceInternal(roleCode, requestedCodes);
        return rolePermissionMapper.toResponse(
                result.roleCode(),
                result.permissionVersion(),
                result.permissionCodes()
        );
    }

    private void rejectSystemAdminPermissionMutation(String roleCode) {
        if (roleCode != null && PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode.trim())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "SYSTEM_ADMIN permissions are system-managed");
        }
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public SyncResult replaceInternal(String roleCode, Set<String> requestedCodes) {
        UUID tenantId = TenantContext.requireTenantId();
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        SortedSet<String> desired = normalizeAndValidate(requestedCodes);
        Role existingRole = roleRepository.findByTenantIdAndCode(tenantId, normalizedRoleCode)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.ROLE_NOT_FOUND,
                        "Role not found: " + normalizedRoleCode));
        SortedSet<String> current = rolePermissionRepository.findPermissionCodes(tenantId, normalizedRoleCode);

        Diff diff = diff(desired, current);
        if (!diff.changed()) {
            return new SyncResult(
                    normalizedRoleCode,
                    existingRole.getPermissionVersion(),
                    Collections.unmodifiableSortedSet(desired),
                    0,
                    0
            );
        }

        Role lockedRole = roleRepository.findByTenantIdAndCodeForUpdate(tenantId, normalizedRoleCode)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.ROLE_NOT_FOUND,
                        "Role not found: " + normalizedRoleCode));
        SortedSet<String> currentAfterLock = rolePermissionRepository.findPermissionCodes(tenantId, normalizedRoleCode);
        Diff diffAfterLock = diff(desired, currentAfterLock);

        applyDiff(tenantId, normalizedRoleCode, lockedRole, diffAfterLock);

        long permissionVersion = lockedRole.getPermissionVersion();
        if (diffAfterLock.changed()) {
            roleRepository.incrementPermissionVersion(tenantId, normalizedRoleCode);
            permissionVersion++;
            lockedRole.setPermissionVersion(permissionVersion);
        }

        return new SyncResult(
                normalizedRoleCode,
                permissionVersion,
                Collections.unmodifiableSortedSet(desired),
                diffAfterLock.toAdd().size(),
                diffAfterLock.toRemove().size()
        );
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public BulkSyncResult replaceAll(Map<String, Set<String>> requestedCodesByRole) {
        if (requestedCodesByRole != null) {
            requestedCodesByRole.keySet().forEach(this::rejectSystemAdminPermissionMutation);
            boolean hasTenantPerm = requestedCodesByRole.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Set::stream)
                    .filter(Objects::nonNull)
                    .map(c -> c.trim().toUpperCase(Locale.ROOT))
                    .anyMatch(PermissionDefinition.TENANT_ADMIN_CODES::contains);
            if (hasTenantPerm) {
                throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "Tenant admin permissions are reserved for SYSTEM_ADMIN");
            }
        }
        return replaceAllInternal(requestedCodesByRole);
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public BulkSyncResult replaceAllInternal(Map<String, Set<String>> requestedCodesByRole) {
        UUID tenantId = TenantContext.requireTenantId();
        if (requestedCodesByRole == null) {
            throw new ApiException(SecurityErrorCode.ROLE_PERMISSIONS_REQUIRED);
        }

        Map<String, SortedSet<String>> desiredByRole = requestedCodesByRole.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> normalizeRoleCode(entry.getKey()),
                        entry -> normalizeAndValidate(entry.getValue()),
                        (left, right) -> {
                            throw new ApiException(SecurityErrorCode.DUPLICATE_ROLE_CODE);
                        },
                        TreeMap::new
                ));

        if (desiredByRole.isEmpty()) {
            return new BulkSyncResult(0, 0);
        }

        Map<String, SortedSet<String>> currentByRole = findCurrentPermissionsByRole(tenantId, desiredByRole.keySet());
        Map<String, Diff> changedDiffs = changedDiffs(desiredByRole, currentByRole);
        if (changedDiffs.isEmpty()) {
            return new BulkSyncResult(0, 0);
        }

        Map<String, Role> lockedRoles = changedDiffs.keySet().stream()
                .map(code -> roleRepository.findByTenantIdAndCodeForUpdate(tenantId, code)
                        .orElseThrow(() -> new ApiException(
                                SecurityErrorCode.ROLE_NOT_FOUND,
                                "Role not found: " + code)))
                .collect(Collectors.toMap(Role::getCode, role -> role));

        Map<String, SortedSet<String>> affectedDesiredByRole = desiredByRole.entrySet().stream()
                .filter(entry -> changedDiffs.containsKey(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        TreeMap::new
                ));
        Map<String, SortedSet<String>> currentAfterLock = findCurrentPermissionsByRole(tenantId, changedDiffs.keySet());
        Map<String, Diff> diffsAfterLock = changedDiffs(affectedDesiredByRole, currentAfterLock);

        int added = 0;
        int removed = 0;
        for (Map.Entry<String, Diff> entry : diffsAfterLock.entrySet()) {
            String code = entry.getKey();
            Diff diff = entry.getValue();
            applyDiff(tenantId, code, lockedRoles.get(code), diff);
            if (diff.changed()) {
                roleRepository.incrementPermissionVersion(tenantId, code);
                added += diff.toAdd().size();
                removed += diff.toRemove().size();
            }
        }

        return new BulkSyncResult(added, removed);
    }

    private RolePermission find(String roleCode, Integer permissionId) {
        UUID tenantId = TenantContext.requireTenantId();
        return rolePermissionRepository.findById(new RolePermission.Key(tenantId, roleCode, permissionId))
                .orElseThrow(() -> new ApiException(SecurityErrorCode.ROLE_PERMISSION_NOT_FOUND));
    }

    private Map<String, SortedSet<String>> findCurrentPermissionsByRole(UUID tenantId, Set<String> roleCodes) {
        Map<String, SortedSet<String>> currentByRole = new TreeMap<>();
        roleCodes.forEach(code -> currentByRole.put(code, new TreeSet<>()));
        rolePermissionRepository.findPermissionCodeRowsByRoleCodeIn(tenantId, roleCodes)
                .forEach(row -> currentByRole
                        .computeIfAbsent(row.getRoleCode(), ignored -> new TreeSet<>())
                        .add(row.getPermissionCode()));
        return currentByRole;
    }

    private Map<String, Diff> changedDiffs(
            Map<String, SortedSet<String>> desiredByRole,
            Map<String, SortedSet<String>> currentByRole
    ) {
        Map<String, Diff> diffs = new TreeMap<>();
        desiredByRole.forEach((code, desired) -> {
            Diff diff = diff(desired, currentByRole.getOrDefault(code, new TreeSet<>()));
            if (diff.changed()) {
                diffs.put(code, diff);
            }
        });
        return diffs;
    }

    private Diff diff(SortedSet<String> desired, SortedSet<String> current) {
        Set<String> toAdd = new TreeSet<>(desired);
        toAdd.removeAll(current);

        Set<String> toRemove = new TreeSet<>(current);
        toRemove.removeAll(desired);

        return new Diff(toAdd, toRemove);
    }

    private void applyDiff(UUID tenantId, String roleCode, Role role, Diff diff) {
        if (!diff.toRemove().isEmpty()) {
            rolePermissionRepository.deleteByTenantIdAndRoleCodeAndPermissionCodeIn(tenantId, roleCode, diff.toRemove());
        }

        if (!diff.toAdd().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllByCodeIn(diff.toAdd());
            if (permissions.size() != diff.toAdd().size()) {
                throw new ApiException(SecurityErrorCode.PERMISSIONS_NOT_FOUND);
            }

            List<RolePermission> rolePermissions = permissions.stream()
                    .map(permission -> new RolePermission(
                            new RolePermission.Key(tenantId, roleCode, permission.getPermissionId()),
                            role,
                            permission
                    ))
                    .toList();
            rolePermissionRepository.saveAll(rolePermissions);
        }
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new ApiException(SecurityErrorCode.ROLE_CODE_REQUIRED);
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private SortedSet<String> normalizeAndValidate(Set<String> requestedCodes) {
        if (requestedCodes == null) {
            throw new ApiException(SecurityErrorCode.PERMISSION_CODES_REQUIRED);
        }

        SortedSet<String> normalized = requestedCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .filter(code -> !code.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> definedCodes = Arrays.stream(PermissionDefinition.values())
                .map(PermissionDefinition::getCode)
                .collect(Collectors.toSet());

        if (!definedCodes.containsAll(normalized)) {
            throw new ApiException(SecurityErrorCode.PERMISSION_CODES_UNDEFINED);
        }

        return normalized;
    }

    public record SyncResult(
            String roleCode,
            long permissionVersion,
            SortedSet<String> permissionCodes,
            int addedCount,
            int removedCount
    ) {
    }

    public record BulkSyncResult(int addedCount, int removedCount) {
    }

    private record Diff(Set<String> toAdd, Set<String> toRemove) {
        boolean changed() {
            return !toAdd.isEmpty() || !toRemove.isEmpty();
        }
    }
}
