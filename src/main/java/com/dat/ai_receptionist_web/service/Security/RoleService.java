package com.dat.ai_receptionist_web.service.Security;

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

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<RoleDTO.SimpleResponse> theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roleList", key = "T(com.dat.ai_receptionist_web.util.CacheKeys).pageable(#pageable)", cacheManager = "redisCacheManager")
    public PageResponse<RoleDTO.SimpleResponse> list(Pageable pageable) {
        var roles = roleRepository.findAll(pageable);
        Map<String, List<RoleRepository.RolePermissionRow>> permissionsByRole =
                permissionRowsByRoleCode(roleCodes(roles.getContent()));

        return PageResponse.of(roles, role -> roleMapper.toSimpleResponse(
                role,
                permissionsByRole.getOrDefault(role.getCode(), List.of())
        ));
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận String id từ caller hoặc request.
     * Output: Trả về RoleDTO.Response theo kết quả xử lý.
     */
    @Transactional(readOnly = true)
    public RoleDTO.Response get(String id) {
        Role role = getRole(id);
        return roleMapper.toResponse(
                role,
                permissionRowsByRoleCode(Set.of(role.getCode())).getOrDefault(role.getCode(), List.of())
        );
    }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận RoleDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về RoleDTO.Response theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RoleDTO.Response create(RoleDTO.CreateRequest request) {
        Role role = new Role();
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissionVersion(request.permissionVersion());
        return roleMapper.toResponse(roleRepository.save(role), List.of());
    }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận String id, RoleDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về RoleDTO.Response theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public RoleDTO.Response update(String id, RoleDTO.UpdateRequest request) {
        Role role = getRole(id);
        roleMapper.updateEntity(request, role);
        Role saved = roleRepository.save(role);
        return roleMapper.toResponse(
                saved,
                permissionRowsByRoleCode(Set.of(saved.getCode())).getOrDefault(saved.getCode(), List.of())
        );
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận String id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public void delete(String id) {
        roleRepository.delete(getRole(id));
    }

    /**
     * Tác dụng: Thực hiện logic getRole của lớp hiện tại.
     * Input: Nhận String code từ caller hoặc request.
     * Output: Trả về Role theo kết quả xử lý.
     */
    public Role getRole(String code) {
        return roleRepository.findById(code)
                .orElseThrow(() -> new ApiException(
                        SecurityErrorCode.ROLE_NOT_FOUND,
                        "Role not found: " + code));
    }

    /**
     * Tác dụng: Kiểm tra sự tồn tại của dữ liệu theo khóa đầu vào.
     * Input: Nhận String code từ caller hoặc request.
     * Output: Trả về true/false thể hiện kết quả kiểm tra hoặc xử lý.
     */
    public boolean exists(String code) {
        return roleRepository.existsById(code);
    }

    /**
     * Tác dụng: Thực hiện logic createRole của lớp hiện tại.
     * Input: Nhận RoleDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về Role theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public Role createRole(RoleDTO.CreateRequest request) {
        String code = request.code();
        String name = request.name();
        String description = request.description();
        if (exists(code)) {
            throw new ApiException(
                    SecurityErrorCode.ROLE_ALREADY_EXISTS,
                    "Role already exists: " + code);
        }
        Role role = roleRepository.save(roleMapper.toEntity(request));

        rolePermissionService.replace(role.getCode(), Set.of(description));

        return roleRepository.save(role);
    }

    /**
     * Tác dụng: Thực hiện logic updateRole của lớp hiện tại.
     * Input: Nhận String code, RoleDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về Role theo kết quả xử lý.
     */
    @Transactional
    @CacheEvict(value = "roleList", allEntries = true, cacheManager = "redisCacheManager")
    public Role updateRole(String code, RoleDTO.UpdateRequest request) {
        Role role = getRole(code);
        roleMapper.updateEntity(request, role);
        return roleRepository.save(role);
    }

    /**
     * Tác dụng: Thực hiện logic deleteRole của lớp hiện tại.
     * Input: Nhận String code từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
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

    private Map<String, List<RoleRepository.RolePermissionRow>> permissionRowsByRoleCode(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Map.of();
        }

        return roleMapper.groupPermissionRowsByRoleCode(
                roleRepository.findPermissionsByRoleCodeIn(roleCodes)
        );
    }
}


