package com.dat.ai_receptionist_web.controller.Notification;

import com.dat.ai_receptionist_web.dto.Notification.NotificationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.service.Notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_READ.getCode())")
    public PageResponse<NotificationDTO.SimpleResponse> list(Pageable pageable) { return notificationService.list(pageable); }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_READ.getCode())")
    public NotificationDTO.Response get(@PathVariable UUID id) { return notificationService.get(id); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_CREATE.getCode())")
    public NotificationDTO.Response create(@Valid @RequestBody NotificationDTO.CreateRequest request) { return notificationService.create(request); }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_UPDATE.getCode())")
    public NotificationDTO.Response update(@PathVariable UUID id, @Valid @RequestBody NotificationDTO.UpdateRequest request) { return notificationService.update(id, request); }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { notificationService.delete(id); }
}


