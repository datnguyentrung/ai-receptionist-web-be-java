package com.dat.ai_receptionist_web.controller.Security;

import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.dto.Security.UserRoleDTO;
import com.dat.ai_receptionist_web.service.Security.UserRoleService;
import com.dat.ai_receptionist_web.service.Security.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
public class UserRoleController {
    private final UserRoleService userRoleService;
    private final UserRoleService Service;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<UserRoleDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_READ.getCode())")
    public PageResponse<UserRoleDTO.SimpleResponse> list(Pageable pageable) { return Service.list(pageable); }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID userId, String roleCode từ caller hoặc request.
     * Output: Trả về UserRoleDTO.ItemResponse theo kết quả xử lý.
     */
    @GetMapping("/{userId}/{roleCode}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_READ.getCode())")
    public UserRoleDTO.ItemResponse get(@PathVariable UUID userId, @PathVariable String roleCode) { return Service.get(userId, roleCode); }

    /**
     * Tác dụng: Gán quan hệ hoặc quyền tương ứng khi điều kiện nghiệp vụ cho phép.
     * Input: Nhận UserRoleDTO.AssignRequest request từ caller hoặc request.
     * Output: Trả về ResponseEntity<UserRoleDTO.Response> theo kết quả xử lý.
     */
    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_CREATE.getCode())")
    public ResponseEntity<UserRoleDTO.Response> assignRole(@RequestBody @Valid UserRoleDTO.AssignRequest request) {
        return ResponseEntity.ok(userRoleService.assignRole(request));
    }

    /**
     * Tác dụng: Thay thế tập dữ liệu hiện tại bằng tập dữ liệu mong muốn theo cơ chế diff.
     * Input: Nhận UUID userId, UserRoleDTO.ReplaceRequest request từ caller hoặc request.
     * Output: Trả về ResponseEntity<UserRoleDTO.Response> theo kết quả xử lý.
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_UPDATE.getCode())")
    public ResponseEntity<UserRoleDTO.Response> replaceRoles(@PathVariable UUID userId, @RequestBody @Valid UserRoleDTO.ReplaceRequest request) {
        return ResponseEntity.ok(userRoleService.replaceRoles(userId, request.getRoleCodes()));
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID userId, String roleCode từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{userId}/{roleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).USER_ROLE_DELETE.getCode())")
    public void delete(@PathVariable UUID userId, @PathVariable String roleCode) { Service.delete(userId, roleCode); }
}


