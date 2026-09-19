package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.*;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Security.UserRoleMapper;
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
    private final UserRoleMapper userRoleMapper;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<UserRoleDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserRoleDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(userRoleRepository.findAllDetailed(pageable), userRoleMapper::toSimpleResponse);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID userId, String roleCode từ caller hoặc request.
     * Output: Trả về UserRoleDTO.ItemResponse theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public UserRoleDTO.ItemResponse get(UUID userId, String roleCode) {
        return userRoleMapper.toResponse(find(userId, roleCode));
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận UserRoleDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về UserRoleDTO.ItemResponse theo kết quả xử lý.
     */
    @Transactional
    public UserRoleDTO.ItemResponse create(UserRoleDTO.CreateRequest request) {
        assignRole(new UserRoleDTO.AssignRequest(request.userId(), request.roleCode()));
        return get(request.userId(), request.roleCode().trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID userId, String roleCode từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    public void delete(UUID userId, String roleCode) {
        userRoleRepository.delete(find(userId, roleCode));
        userRepository.incrementAuthorizationVersion(userId);
    }

    /**
     * Tác dụng: Gán quan hệ hoặc quyền tương ứng khi điều kiện nghiệp vụ cho phép.
     * Input: Nhận UserRoleDTO.AssignRequest request từ caller hoặc request.
     * Output: Trả về UserRoleDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public UserRoleDTO.Response assignRole(UserRoleDTO.AssignRequest request) {
        UUID userId = request.getUserId();
        assignRoleIfMissing(userId, request.getRoleCode());
        return userRoleMapper.toResponse(userId, userRoleRepository.findRoleCodes(userId));
    }

    /**
     * Tác dụng: Gán quan hệ hoặc quyền tương ứng khi điều kiện nghiệp vụ cho phép.
     * Input: Nhận UUID userId, String requestedRoleCode từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    @Transactional
    public boolean assignRoleIfMissing(UUID userId, String requestedRoleCode) {
        return assignRolesIfMissing(Map.of(userId, Set.of(requestedRoleCode))) > 0;
    }

    /**
     * Tác dụng: Gán quan hệ hoặc quyền tương ứng khi điều kiện nghiệp vụ cho phép.
     * Input: Nhận Map<UUID, Set<String>> requestedRoleCodesByUser từ caller hoặc request.
     * Output: Trả về giá trị int biểu thị kết quả tính toán hoặc số lượng.
     */
    @Transactional
    public int assignRolesIfMissing(Map<UUID, Set<String>> requestedRoleCodesByUser) {
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
        Set<UserRoleKey> currentAssignments = findUserRoleKeys(userIds);
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

        Set<UserRoleKey> currentAfterLock = findUserRoleKeys(affectedUserIds);
        List<UserRoleKey> toCreate = missingAssignments.stream()
                .filter(key -> !currentAfterLock.contains(key))
                .toList();
        if (toCreate.isEmpty()) {
            return 0;
        }

        Set<String> roleCodes = toCreate.stream()
                .map(UserRoleKey::roleCode)
                .collect(Collectors.toSet());
        Map<String, Role> rolesByCode = roleRepository.findAllById(roleCodes).stream()
                .collect(Collectors.toMap(Role::getCode, Function.identity()));
        validateRolesExist(roleCodes, rolesByCode);

        List<UserRole> userRoles = toCreate.stream()
                .map(key -> new UserRole(
                        new UserRole.Key(key.userId(), key.roleCode()),
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

    /**
     * Tác dụng: Thay thế tập dữ liệu hiện tại bằng tập dữ liệu mong muốn theo cơ chế diff.
     * Input: Nhận UUID userId, Set<String> requestedCodes từ caller hoặc request.
     * Output: Trả về UserRoleDTO.Response theo kết quả xử lý.
     */
    @Transactional
    public UserRoleDTO.Response replaceRoles(
            UUID userId,
            Set<String> requestedCodes
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new ApiException(SecurityErrorCode.USER_NOT_FOUND)
                );

        SortedSet<String> desired = requestedCodes.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));

        SortedSet<String> current =
                userRoleRepository.findRoleCodes(userId);

        Set<String> toAdd = new HashSet<>(desired);
        toAdd.removeAll(current);

        Set<String> toRemove = new HashSet<>(current);
        toRemove.removeAll(desired);

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {

            if (!toRemove.isEmpty()) {
                userRoleRepository.deleteById_UserIdAndRole_CodeIn(
                        userId,
                        toRemove
                );
            }

            if (!toAdd.isEmpty()) {
                List<Role> roles = roleRepository.findAllById(toAdd);

                if (roles.size() != toAdd.size()) {
                    throw new ApiException(
                            SecurityErrorCode.ROLES_NOT_FOUND);
                }

                List<UserRole> userRoles = roles.stream()
                        .map(role ->
                                new UserRole(
                                        new UserRole.Key(
                                                userId,
                                                role.getCode()
                                        ),
                                        user,
                                        role
                                )
                        )
                        .toList();

                userRoleRepository.saveAll(userRoles);
            }

            userRepository.incrementAuthorizationVersion(userId);
        }

        return userRoleMapper.toResponse(userId, desired);
    }

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận UUID userId, String roleCode từ caller hoặc request.
     * Output: Trả về UserRole theo kết quả xử lý.
     */
    private UserRole find(UUID userId, String roleCode) {
        return userRoleRepository.findById(new UserRole.Key(userId, roleCode))
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_ROLE_NOT_FOUND));
    }

    /**
     * Tác dụng: Chuẩn hóa dữ liệu đầu vào về định dạng thống nhất để so sánh và lưu trữ.
     * Input: Nhận Set<String> roleCodes từ caller hoặc request.
     * Output: Trả về SortedSet<String> theo kết quả xử lý.
     */
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

    /**
     * Tác dụng: Tìm và trả về dữ liệu nội bộ theo điều kiện đầu vào.
     * Input: Nhận Set<UUID> userIds từ caller hoặc request.
     * Output: Trả về Set<UserRoleKey> theo kết quả xử lý.
     */
    private Set<UserRoleKey> findUserRoleKeys(Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return userRoleRepository.findAllByUserIdIn(userIds).stream()
                .map(row -> new UserRoleKey(row.getUserId(), row.getRoleCode()))
                .collect(Collectors.toSet());
    }

    /**
     * Tác dụng: Kiểm tra tính hợp lệ của dữ liệu đầu vào trước khi xử lý tiếp.
     * Input: Nhận Set<UUID> userIds, Map<UUID, User> usersById từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    private void validateUsersExist(Set<UUID> userIds, Map<UUID, User> usersById) {
        Set<UUID> missingUserIds = new HashSet<>(userIds);
        missingUserIds.removeAll(usersById.keySet());
        if (!missingUserIds.isEmpty()) {
            throw new ApiException(
                    SecurityErrorCode.USER_NOT_FOUND,
                    "User not found: " + missingUserIds);
        }
    }

    /**
     * Tác dụng: Kiểm tra tính hợp lệ của dữ liệu đầu vào trước khi xử lý tiếp.
     * Input: Nhận Set<String> roleCodes, Map<String, Role> rolesByCode từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
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


