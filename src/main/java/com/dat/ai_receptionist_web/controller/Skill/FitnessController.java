package com.dat.ai_receptionist_web.controller.Skill;

import com.dat.ai_receptionist_web.dto.Skill.FitnessDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Skill.FitnessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fitness")
@RequiredArgsConstructor
public class FitnessController {
    private final FitnessService service;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<FitnessDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_READ.getCode())")
    public PageResponse<FitnessDTO.SimpleResponse> list(Pageable pageable) { return service.list(pageable); }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Trả về FitnessDTO.Response theo kết quả xử lý.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_READ.getCode())")
    public FitnessDTO.Response get(@PathVariable Long id) { return service.get(id); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận FitnessDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về FitnessDTO.Response theo kết quả xử lý.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_CREATE.getCode())")
    public FitnessDTO.Response create(@Valid @RequestBody FitnessDTO.CreateRequest request) { return service.create(request); }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận Long id, FitnessDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về FitnessDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_UPDATE.getCode())")
    public FitnessDTO.Response update(@PathVariable Long id, @Valid @RequestBody FitnessDTO.UpdateRequest request) { return service.update(id, request); }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận Long id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).FITNESS_DELETE.getCode())")
    public void delete(@PathVariable Long id) { service.delete(id); }
}


