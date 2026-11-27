package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.context.TenantContext;
import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.enums.Core.TenantMembershipStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Security.UserRoleMapper;
import com.dat.ai_receptionist_web.repository.Core.TenantMembershipRepository;
import com.dat.ai_receptionist_web.repository.Security.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final UserRoleMapper userRoleMapper;
    private final PlatformOwnerService platformOwnerService;

    @Transactional(readOnly = true)
    public PageResponse<UserRoleDTO.SimpleResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        var page = platformOwnerService.isCurrentUserPlatformOwner()
                ? userRoleRepository.findAllDetailedByTenantId(tenantId, pageable)
                : userRoleRepository.findAllDetailedByTenantIdAndRoleCodeNot(
                        tenantId, PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE, pageable);
        return PageResponse.of(page, userRoleMapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public UserRoleDTO.ItemResponse get(UUID userId, String roleCode) {
        if (!platformOwnerService.isCurrentUserPlatformOwner()
                && PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode)) {
            throw new ApiException(SecurityErrorCode.USER_ROLE_NOT_FOUND);
        }
        return userRoleMapper.toResponse(find(userId, roleCode));
    }

    @Transactional
    public UserRoleDTO.ItemResponse create(UserRoleDTO.CreateRequest request) {
        assignRole(new UserRoleDTO.AssignRequest(request.userId(), request.roleCode()));
        return get(request.userId(), request.roleCode().trim().toUpperCase(Locale.ROOT));
    }

    @Transactional
    public void delete(UUID userId, String roleCode) {
        rejectSystemAdminAssignment(roleCode);
        userRoleRepository.delete(find(userId, roleCode));
        userRepository.incrementAuthorizationVersion(userId);
    }

    @Transactional
    public UserRoleDTO.Response assignRole(UserRoleDTO.AssignRequest request) {
        rejectSystemAdminAssignment(request.getRoleCode());
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = request.getUserId();
        assignRoleIfMissing(userId, request.getRoleCode());
        return userRoleMapper.toResponse(userId, userRoleRepository.findRoleCodes(tenantId, userId));
    }

    private void rejectSystemAdminAssignment(String roleCode) {
        if (roleCode != null && PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode.trim())) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "SYSTEM_ADMIN role cannot be assigned or revoked via API");
        }
    }

    @Transactional
    public boolean assignRoleIfMissing(UUID userId, String requestedRoleCode) {
        return assignRolesIfMissing(Map.of(userId, Set.of(requestedRoleCode))) > 0;
    }

    @Transactional
    public int assignRolesIfMissing(Map<UUID, Set<String>> requestedRoleCodesByUser) {
        UUID tenantId = TenantContext.requireTenantId();
        if (requestedRoleCodesByUser == null || requestedRoleCodesByUser.isEmpty()) {
            return 0;
        }

        Map<UUID, SortedSet<String>> desiredRoleCodesByUser = requestedRoleCodesByUser.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> normalizeRoleCodes(entry.getValue()),
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        },
                        LinkedHashMap::new
                ));

        Set<UUID> userIds = desiredRoleCodesByUser.keySet();
        Set<UserRoleKey> currentAssignments = findUserRoleKeys(tenantId, userIds);
        Set<UserRoleKey> desiredAssignments = desiredRoleCodesByUser.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(roleCode -> new UserRoleKey(entry.getKey(), roleCode)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<UserRoleKey> missingAssignments = new LinkedHashSet<>(desiredAssignments);
        missingAssignments.removeAll(currentAssignments);
        if (missingAssignments.isEmpty()) {
            return 0;
        }

        Set<UUID> affectedUserIds = missingAssignments.stream()
                .map(UserRoleKey::userId)
                .collect(Collectors.toSet());
        Map<UUID, User> lockedUsers = userRepository.findAllByUserIdInForUpdate(affectedUserIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        validateUsersExist(affectedUserIds, lockedUsers);
        for (UUID affectedUserId : affectedUserIds) {
            ensureActiveMembership(tenantId, affectedUserId);
        }

        Set<UserRoleKey> currentAfterLock = findUserRoleKeys(tenantId, affectedUserIds);
        List<UserRoleKey> toCreate = missingAssignments.stream()
                .filter(key -> !currentAfterLock.contains(key))
                .toList();
        if (toCreate.isEmpty()) {
            return 0;
        }

        Set<String> roleCodes = toCreate.stream()
                .map(UserRoleKey::roleCode)
                .collect(Collectors.toSet());
        Map<String, Role> rolesByCode = roleRepository.findAllByTenantIdAndCodeIn(tenantId, roleCodes).stream()
                .collect(Collectors.toMap(Role::getCode, Function.identity()));
        validateRolesExist(roleCodes, rolesByCode);

        List<UserRole> userRoles = toCreate.stream()
                .map(key -> new UserRole(
                        new UserRole.Key(tenantId, key.userId(), key.roleCode()),
                        lockedUsers.get(key.userId()),
                        rolesByCode.get(key.roleCode())
                ))
                .toList();

        userRoleRepository.saveAll(userRoles);
        toCreate.stream()
                .map(UserRoleKey::userId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .forEach(userRepository::incrementAuthorizationVersion);
        return userRoles.size();
    }

    @Transactional
    public UserRoleDTO.Response replaceRoles(
            UUID userId,
            Set<String> requestedCodes
    ) {
        if (requestedCodes != null) {
            requestedCodes.forEach(this::rejectSystemAdminAssignment);
        }
        UUID tenantId = TenantContext.requireTenantId();
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
        ensureActiveMembership(tenantId, userId);

        SortedSet<String> desired = requestedCodes.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));

        SortedSet<String> current = userRoleRepository.findRoleCodes(tenantId, userId);
        if (current.contains(PlatformOwnerService.SYSTEM_ADMIN_ROLE_CODE)) {
            throw new ApiException(SecurityErrorCode.ACCESS_DENIED, "SYSTEM_ADMIN role cannot be modified via API");
        }

        Set<String> toAdd = new HashSet<>(desired);
        toAdd.removeAll(current);

        Set<String> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            if (!toRemove.isEmpty()) {
                userRoleRepository.deleteById_TenantIdAndId_UserIdAndId_RoleIdIn(
                        tenantId,
                        userId,
                        toRemove
                );
            }

            if (!toAdd.isEmpty()) {
                List<Role> roles = roleRepository.findAllByTenantIdAndCodeIn(tenantId, toAdd);

                if (roles.size() != toAdd.size()) {
                    throw new ApiException(SecurityErrorCode.ROLES_NOT_FOUND);
                }

                List<UserRole> userRoles = roles.stream()
                        .map(role -> new UserRole(
                                new UserRole.Key(tenantId, userId, role.getCode()),
                                user,
                                role
                        ))
                        .toList();

                userRoleRepository.saveAll(userRoles);
            }

            userRepository.incrementAuthorizationVersion(userId);
        }

        return userRoleMapper.toResponse(userId, desired);
    }

    private void ensureActiveMembership(UUID tenantId, UUID userId) {
        if (!tenantMembershipRepository.existsByTenantIdAndUser_UserIdAndStatus(
                tenantId, userId, TenantMembershipStatus.ACTIVE)) {
            throw new ApiException(SecurityErrorCode.TENANT_MEMBERSHIP_REVOKED);
        }
    }

    private UserRole find(UUID userId, String roleCode) {
        UUID tenantId = TenantContext.requireTenantId();
        return userRoleRepository.findById(new UserRole.Key(tenantId, userId, roleCode))
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_ROLE_NOT_FOUND));
    }

    private SortedSet<String> normalizeRoleCodes(Set<String> roleCodes) {
        if (roleCodes == null) {
            throw new ApiException(SecurityErrorCode.ROLE_CODES_REQUIRED);
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(roleCode -> roleCode.toUpperCase(Locale.ROOT))
                .filter(roleCode -> !roleCode.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<UserRoleKey> findUserRoleKeys(UUID tenantId, Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return userRoleRepository.findAllByTenantIdAndUserIdIn(tenantId, userIds).stream()
                .map(row -> new UserRoleKey(row.getUserId(), row.getRoleCode()))
                .collect(Collectors.toSet());
    }

    private void validateUsersExist(Set<UUID> userIds, Map<UUID, User> usersById) {
        Set<UUID> missingUserIds = new HashSet<>(userIds);
        missingUserIds.removeAll(usersById.keySet());
        if (!missingUserIds.isEmpty()) {
            throw new ApiException(
                    SecurityErrorCode.USER_NOT_FOUND,
                    "User not found: " + missingUserIds);
        }
    }

    private void validateRolesExist(Set<String> roleCodes, Map<String, Role> rolesByCode) {
        Set<String> missingRoleCodes = new TreeSet<>(roleCodes);
        missingRoleCodes.removeAll(rolesByCode.keySet());
        if (!missingRoleCodes.isEmpty()) {
            throw new ApiException(
                    SecurityErrorCode.ROLE_NOT_FOUND,
                    "Role not found: " + missingRoleCodes);
        }
    }

    private record UserRoleKey(UUID userId, String roleCode) {
    }
}
