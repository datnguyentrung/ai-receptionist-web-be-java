package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.service.Security.RolePermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RolePermissionController {
    private final RolePermissionService rolePermissionService;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<RolePermissionDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping("/api/v1/role-permissions")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_READ.getCode())")
    public PageResponse<RolePermissionDTO.SimpleResponse> list(Pageable pageable) { return rolePermissionService.list(pageable); }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận String roleCode, Integer permissionId từ caller hoặc request.
     * Output: Trả về RolePermissionDTO.ItemResponse theo kết quả xử lý.
     */
    @GetMapping("/api/v1/role-permissions/{roleCode}/{permissionId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_READ.getCode())")
    public RolePermissionDTO.ItemResponse get(@PathVariable String roleCode, @PathVariable Integer permissionId) { return rolePermissionService.get(roleCode, permissionId); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận RolePermissionDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về RolePermissionDTO.ItemResponse theo kết quả xử lý.
     */
    @PostMapping("/api/v1/role-permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_CREATE.getCode())")
    public RolePermissionDTO.ItemResponse create(@Valid @RequestBody RolePermissionDTO.CreateRequest request) { return rolePermissionService.create(request); }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận String roleCode, Integer permissionId từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/api/v1/role-permissions/{roleCode}/{permissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_DELETE.getCode())")
    public void delete(@PathVariable String roleCode, @PathVariable Integer permissionId) { rolePermissionService.delete(roleCode, permissionId); }

    /**
     * Tác dụng: Thay thế tập dữ liệu hiện tại bằng tập dữ liệu mong muốn theo cơ chế diff.
     * Input: Nhận String roleCode, RolePermissionDTO.ReplaceRequest request từ caller hoặc request.
     * Output: Trả về RolePermissionDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/api/v1/roles/{roleCode}/permissions")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).ROLE_PERMISSION_UPDATE.getCode())")
    public RolePermissionDTO.Response replace(@PathVariable String roleCode, @Valid @RequestBody RolePermissionDTO.ReplaceRequest request) {
        return rolePermissionService.replace(roleCode, request.permissionCodes());
    }
}


