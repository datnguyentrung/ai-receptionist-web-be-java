package com.dat.ai_receptionist_web.controller.Notification;

import com.dat.ai_receptionist_web.dto.Notification.NotificationRecipientDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.Training.NotificationType;
import com.dat.ai_receptionist_web.service.Notification.NotificationRecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-recipients")
@RequiredArgsConstructor
public class NotificationRecipientController {
    private final NotificationRecipientService service;

    /**
     * Tác dụng: Lấy danh sách bản ghi theo điều kiện phân trang.
     * Input: Nhận Pageable pageable từ caller hoặc request.
     * Output: Trả về PageResponse<NotificationRecipientDTO.SimpleResponse> theo kết quả xử lý.
     */
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_READ.getCode())")
    public PageResponse<NotificationRecipientDTO.SimpleResponse> list(Pageable pageable) { return service.list(pageable); }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public PageResponse<NotificationRecipientDTO.MineResponse> listMine(
            Pageable pageable,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) String search) {
        return service.listMine(read, type, search, pageable);
    }

    @GetMapping("/mine/{id}")
    @PreAuthorize("isAuthenticated()")
    public NotificationRecipientDTO.MineResponse getMine(@PathVariable UUID id) {
        return service.getMine(id);
    }

    @GetMapping("/mine/unread-count")
    @PreAuthorize("isAuthenticated()")
    public NotificationRecipientDTO.UnreadCountResponse unreadCountMine() {
        return service.unreadCountMine();
    }

    @PatchMapping("/mine/read-all")
    @PreAuthorize("isAuthenticated()")
    public NotificationRecipientDTO.UnreadCountResponse markAllRead() {
        return service.markAllRead();
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public NotificationRecipientDTO.UnreadCountResponse markRead(@PathVariable UUID id) {
        return service.markRead(id);
    }

    /**
     * Tác dụng: Lấy chi tiết một bản ghi theo khóa định danh.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_READ.getCode())")
    public NotificationRecipientDTO.Response get(@PathVariable UUID id) { return service.get(id); }

    /**
     * Tác dụng: Tạo mới bản ghi và trả về dữ liệu sau khi tạo.
     * Input: Nhận NotificationRecipientDTO.CreateRequest request từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_CREATE.getCode())")
    public NotificationRecipientDTO.Response create(@Valid @RequestBody NotificationRecipientDTO.CreateRequest request) { return service.create(request); }

    /**
     * Tác dụng: Cập nhật bản ghi hiện có và trả về dữ liệu sau khi cập nhật.
     * Input: Nhận UUID id, NotificationRecipientDTO.UpdateRequest request từ caller hoặc request.
     * Output: Trả về NotificationRecipientDTO.Response theo kết quả xử lý.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_UPDATE.getCode())")
    public NotificationRecipientDTO.Response update(@PathVariable UUID id, @Valid @RequestBody NotificationRecipientDTO.UpdateRequest request) { return service.update(id, request); }

    /**
     * Tác dụng: Xóa hoặc vô hiệu hóa bản ghi theo định danh đầu vào.
     * Input: Nhận UUID id từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority(T(com.dat.ai_receptionist_web.enums.Security.PermissionDefinition).NOTIFICATION_RECIPIENT_DELETE.getCode())")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}


