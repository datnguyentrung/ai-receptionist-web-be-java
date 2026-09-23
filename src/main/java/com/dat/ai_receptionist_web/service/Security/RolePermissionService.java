package com.dat.ai_receptionist_web.service.Security;

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

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<RolePermissionDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<RolePermissionDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(rolePermissionRepository.findAllDetailed(pageable), rolePermissionMapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận String roleCode, Integer permissionId từ caller hoặc request.
     * Output: Trả về RolePermissionDTO.ItemResponse theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public RolePermissionDTO.ItemResponse get(String roleCode, Integer permissionId) {
        return rolePermissionMapper.toResponse(find(roleCode, permissionId));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận RolePermissionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về RolePermissionDTO.ItemResponse theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RolePermissionDTO.ItemResponse create(RolePermissionDTO.CreateRequest request) {
        Role role = roleRepository.findById(request.roleCode())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.ROLE_NOT_FOUND));
        Permission permission = permissionRepository.findById(request.permissionId())
                .orElseThrow(() -> new ApiException(SecurityErrorCode.PERMISSION_NOT_FOUND));
        RolePermission entity = new RolePermission(
                new RolePermission.Key(role.getCode(), permission.getPermissionId()), role, permission);
        RolePermission saved = rolePermissionRepository.save(entity);
        roleRepository.incrementPermissionVersion(role.getCode());
        return rolePermissionMapper.toResponse(saved);
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận String roleCode, Integer permissionId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public void delete(String roleCode, Integer permissionId) {
        rolePermissionRepository.delete(find(roleCode, permissionId));
        roleRepository.incrementPermissionVersion(roleCode);
    }

    /**
     * Tác dụng: Thay thế tập dữ liệu hiện tại bằng tập dữ liệu mong muốn theo cơ chế diff.
     * Input: Nhận String roleCode, Set<String> requestedCodes từ caller hoặc request.
     * Output: Trả về RolePermissionDTO.Response theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RolePermissionDTO.Response replace(String roleCode, Set<String> requestedCodes) {
        SyncResult result = replaceInternal(roleCode, requestedCodes);
        return rolePermissionMapper.toResponse(
                result.roleCode(),
                result.permissionVersion(),
                result.permissionCodes()
        );
    }

    /**
     * Tác dụng: Thay thế tập dữ liệu hiện tại bằng tập dữ liệu mong muốn theo cơ chế diff.
     * Input: Nhận String roleCode, Set<String> requestedCodes từ caller hoặc request.
     * Output: Trả về SyncResult theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public SyncResult replaceInternal(String roleCode, Set<String> requestedCodes) {
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        SortedSet<String> desired = normalizeAndValidate(requestedCodes);
        Role existingRole = roleRepository.findById(normalizedRoleCode)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.ROLE_NOT_FOUND,
                        "Role not found: " + normalizedRoleCode));
        SortedSet<String> current = rolePermissionRepository.findPermissionCodes(normalizedRoleCode);

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

        Role lockedRole = roleRepository.findByIdForUpdate(normalizedRoleCode)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.ROLE_NOT_FOUND,
                        "Role not found: " + normalizedRoleCode));
        SortedSet<String> currentAfterLock = rolePermissionRepository.findPermissionCodes(normalizedRoleCode);
        Diff diffAfterLock = diff(desired, currentAfterLock);

        applyDiff(normalizedRoleCode, lockedRole, diffAfterLock);

        long permissionVersion = lockedRole.getPermissionVersion();
        if (diffAfterLock.changed()) {
            roleRepository.incrementPermissionVersion(normalizedRoleCode);
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

    /**
     * Tác dụng: Thay thế tập dữ liệu hiện tại bằng tập dữ liệu mong muốn theo cơ chế diff.
     * Input: Nhận Map<String, Set<String>> requestedCodesByRole từ caller hoặc request.
     * Output: Trả về BulkSyncResult theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public BulkSyncResult replaceAll(Map<String, Set<String>> requestedCodesByRole) {
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

        Map<String, SortedSet<String>> currentByRole = findCurrentPermissionsByRole(desiredByRole.keySet());
        Map<String, Diff> changedDiffs = changedDiffs(desiredByRole, currentByRole);
        if (changedDiffs.isEmpty()) {
            return new BulkSyncResult(0, 0);
        }

        Map<String, Role> lockedRoles = changedDiffs.keySet().stream()
                .map(code -> roleRepository.findByIdForUpdate(code)
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
        Map<String, SortedSet<String>> currentAfterLock = findCurrentPermissionsByRole(changedDiffs.keySet());
        Map<String, Diff> diffsAfterLock = changedDiffs(affectedDesiredByRole, currentAfterLock);

        int added = 0;
        int removed = 0;
        for (Map.Entry<String, Diff> entry : diffsAfterLock.entrySet()) {
            String code = entry.getKey();
            Diff diff = entry.getValue();
            applyDiff(code, lockedRoles.get(code), diff);
            if (diff.changed()) {
                roleRepository.incrementPermissionVersion(code);
                added += diff.toAdd().size();
                removed += diff.toRemove().size();
            }
        }

        return new BulkSyncResult(added, removed);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận String roleCode, Integer permissionId từ caller hoặc request.
     * Output: Trả về RolePermission theo kết quả xử lý.
     */
    private RolePermission find(String roleCode, Integer permissionId) {
        return rolePermissionRepository.findById(new RolePermission.Key(roleCode, permissionId))
                .orElseThrow(() -> new ApiException(SecurityErrorCode.ROLE_PERMISSION_NOT_FOUND));
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận Set<String> roleCodes từ caller hoặc request.
     * Output: Trả về SortedSet<String>> theo kết quả xử lý.
     */
    private Map<String, SortedSet<String>> findCurrentPermissionsByRole(Set<String> roleCodes) {
        Map<String, SortedSet<String>> currentByRole = new TreeMap<>();
        roleCodes.forEach(code -> currentByRole.put(code, new TreeSet<>()));
        rolePermissionRepository.findPermissionCodeRowsByRoleCodeIn(roleCodes)
                .forEach(row -> currentByRole
                        .computeIfAbsent(row.getRoleCode(), ignored -> new TreeSet<>())
                        .add(row.getPermissionCode()));
        return currentByRole;
    }

    /**
     * Tác dụng: Thực hiện logic changedDiffs của lớp hiện tại.
     * Input: Nhận Map<String, SortedSet<String>> desiredByRole, Map<String, SortedSet<String>> currentByRole từ caller hoặc request.
     * Output: Trả về Diff> theo kết quả xử lý.
     */
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

    /**
     * Tác dụng: Thực hiện logic diff của lớp hiện tại.
     * Input: Nhận SortedSet<String> desired, SortedSet<String> current từ caller hoặc request.
     * Output: Trả về Diff theo kết quả xử lý.
     */
    private Diff diff(SortedSet<String> desired, SortedSet<String> current) {
        Set<String> toAdd = new TreeSet<>(desired);
        toAdd.removeAll(current);

        Set<String> toRemove = new TreeSet<>(current);
        toRemove.removeAll(desired);

        return new Diff(toAdd, toRemove);
    }

    /**
     * Tác dụng: Thực hiện logic applyDiff của lớp hiện tại.
     * Input: Nhận String roleCode, Role role, Diff diff từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void applyDiff(String roleCode, Role role, Diff diff) {
        if (!diff.toRemove().isEmpty()) {
            rolePermissionRepository.deleteByRoleCodeAndPermissionCodeIn(roleCode, diff.toRemove());
        }

        if (!diff.toAdd().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllByCodeIn(diff.toAdd());
            if (permissions.size() != diff.toAdd().size()) {
                throw new ApiException(SecurityErrorCode.PERMISSIONS_NOT_FOUND);
            }

            List<RolePermission> rolePermissions = permissions.stream()
                    .map(permission -> new RolePermission(
                            new RolePermission.Key(roleCode, permission.getPermissionId()),
                            role,
                            permission
                    ))
                    .toList();
            rolePermissionRepository.saveAll(rolePermissions);
        }
    }

    /**
     * Tác dụng: Chuẩn hóa dữ liệu đầu vào về định dạng thống nhất để so sánh và lưu trữ.
     * Input: Nhận String roleCode từ caller hoặc request.
     * Output: Trả về String theo kết quả xử lý.
     */
    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new ApiException(SecurityErrorCode.ROLE_CODE_REQUIRED);
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Tác dụng: Chuẩn hóa dữ liệu đầu vào về định dạng thống nhất để so sánh và lưu trữ.
     * Input: Nhận Set<String> requestedCodes từ caller hoặc request.
     * Output: Trả về SortedSet<String> theo kết quả xử lý.
     */
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


