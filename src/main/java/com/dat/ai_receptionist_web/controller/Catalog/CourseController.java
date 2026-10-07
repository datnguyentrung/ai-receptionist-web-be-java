package com.dat.ai_receptionist_web.controller.Catalog;

import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Catalog.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService service;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<CourseDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_READ.getCode())")
    public PageResponse<CourseDTO.SimpleResponse> list(Pageable pageable) { return service.list(pageable); }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về CourseDTO.Response theo kết quả xử lý.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_READ.getCode())")
    public CourseDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận CourseDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về CourseDTO.Response theo kết quả xử lý.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_CREATE.getCode())")
    public CourseDTO.Response create(@Valid @RequestBody CourseDTO.CreateRequest request) { return service.create(request); }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, CourseDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về CourseDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_UPDATE.getCode())")
    public CourseDTO.Response update(@PathVariable UUID id, @Valid @RequestBody CourseDTO.UpdateRequest request) { return service.update(id, request); }

    @PostMapping("/{id}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_UPDATE.getCode())")
    public com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.Response addSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.UpsertRequest request) {
        return service.addSchedule(id, request);
    }

    @PutMapping("/{courseId}/schedules/{scheduleId}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_UPDATE.getCode())")
    public com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.Response updateSchedule(
            @PathVariable UUID courseId, @PathVariable UUID scheduleId,
            @Valid @RequestBody com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.UpsertRequest request) {
        return service.updateSchedule(courseId, scheduleId, request);
    }

    @DeleteMapping("/{courseId}/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_UPDATE.getCode())")
    public void deactivateSchedule(@PathVariable UUID courseId, @PathVariable UUID scheduleId) {
        service.deactivateSchedule(courseId, scheduleId);
    }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).COURSE_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}


