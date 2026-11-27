package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Security.RoleMapper;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final RolePermissionService rolePermissionService;
    private final PlatformOwnerService platformOwnerService;

    @Transactional(readOnly = true)
    @Cacheable(
            value = "roleList",
            key = "T(com.dat.ai_receptionist_web.util.CacheKeys).pageable(#pageable) + ':' + (@platformOwnerService.isCurrentUserPlatformOwner() ? 'owner' : 'std')",
            cacheManager = "redisCacheManager")
    public PageResponse<RoleDTO.SimpleResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        boolean isOwner = platformOwnerService.isCurrentUserPlatformOwner();
        var roles = isOwner
                ? roleRepository.findAllByTenantId(tenantId, pageable)
                : roleRepository.findAllByTenantIdAndCodeNot(tenantId, PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE, pageable);
        Map<String, List<RoleRepository.RolePermissionRow>> permissionsByRole =
                permissionRowsByRoleCode(tenantId, roleCodes(roles.getContent()));

        return PageResponse.of(roles, role -> roleMapper.toSimpleResponse(
                role,
                permissionsByRole.getOrDefault(role.getCode(), List.of())
        ));
    }

    @Transactional(readOnly = true)
    public RoleDTO.Response get(String id) {
        if (!platformOwnerService.isCurrentUserPlatformOwner()
                && PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE.equalsIgnoreCase(id)) {
            throw new ApiException(SecurityErrorCode.ROLE_NOT_FOUND, "Role not found: " + id);
        }
        UUID tenantId = TenantContext.requireTenantId();
        Role role = getRole(tenantId, id);
        return roleMapper.toResponse(
                role,
                permissionRowsByRoleCode(tenantId, Set.of(role.getCode())).getOrDefault(role.getCode(), List.of())
        );
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RoleDTO.Response create(RoleDTO.CreateRequest request) {
        rejectSystemAdminMutation(request.code());
        UUID tenantId = TenantContext.requireTenantId();
        if (roleRepository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new ApiException(SecurityErrorCode.ROLE_ALREADY_EXISTS, "Role already exists: " + request.code());
        }
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissionVersion(request.permissionVersion());
        return roleMapper.toResponse(roleRepository.save(role), List.of());
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RoleDTO.Response update(String id, RoleDTO.UpdateRequest request) {
        rejectSystemAdminMutation(id);
        UUID tenantId = TenantContext.requireTenantId();
        Role role = getRole(tenantId, id);
        roleMapper.updateEntity(request, role);
        Role saved = roleRepository.save(role);
        return roleMapper.toResponse(
                saved,
                permissionRowsByRoleCode(tenantId, Set.of(saved.getCode())).getOrDefault(saved.getCode(), List.of())
        );
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public void delete(String id) {
        rejectSystemAdminMutation(id);
        roleRepository.delete(getRole(id));
    }

    private void rejectSystemAdminMutation(String code) {
        if (code != null && PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE.equalsIgnoreCase(code.trim())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "SYSTEM_ADMIN role is system-managed");
        }
    }

    public Role getRole(String code) {
        return getRole(TenantContext.requireTenantId(), code);
    }

    public Role getRole(UUID tenantId, String code) {
        return roleRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.ROLE_NOT_FOUND,
                        "Role not found: " + code));
    }

    public boolean exists(String code) {
        return roleRepository.existsByTenantIdAndCode(TenantContext.requireTenantId(), code);
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public Role createRole(RoleDTO.CreateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String code = request.code();
        if (roleRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new ApiException(
                    SecurityErrorCode.ROLE_ALREADY_EXISTS,
                    "Role already exists: " + code);
        }
        Role entity = roleMapper.toEntity(request);
        entity.setTenantId(tenantId);
        Role role = roleRepository.save(entity);
        if (request.description() != null && !request.description().isBlank()) {
            rolePermissionService.replace(role.getCode(), Set.of(request.description()));
        }
        return roleRepository.save(role);
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public Role updateRole(String code, RoleDTO.UpdateRequest request) {
        Role role = getRole(code);
        roleMapper.updateEntity(request, role);
        return roleRepository.save(role);
    }

    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public void deleteRole(String code) {
        Role role = getRole(code);
        roleRepository.delete(role);
    }

    private Set<String> roleCodes(List<Role> roles) {
        return roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, List<RoleRepository.RolePermissionRow>> permissionRowsByRoleCode(
            UUID tenantId, Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Map.of();
        }

        return roleMapper.groupPermissionRowsByRoleCode(
                roleRepository.findPermissionsByTenantIdAndRoleCodeIn(tenantId, roleCodes)
        );
    }
}
