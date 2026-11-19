package com.dat.ai_receptionist_web.domain.Training.command;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lệnh điểm danh được xử lý bất đồng bộ, lưu trạng thái và kết quả theo mã yêu cầu.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_command", schema = "training")
public class AttendanceCommand {
    /** Mã yêu cầu dùng để định danh lệnh và bảo đảm xử lý idempotent. */
    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    /** Loại thao tác điểm danh cần thực hiện. */
    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 64)
    private AttendanceCommandType commandType;

    /** Trạng thái xử lý hiện tại của lệnh điểm danh. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AttendanceCommandStatus status;

    /** Dữ liệu đầu vào của lệnh, thường được lưu dưới dạng JSON. */
    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    /** Kết quả xử lý của lệnh, thường được lưu dưới dạng JSON. */
    @Column(name = "result", columnDefinition = "text")
    private String result;

    /** Mã lỗi khi lệnh xử lý thất bại hoặc bị từ chối. */
    @Column(name = "error_code", length = 128)
    private String errorCode;

    /** Tiêu đề lỗi hiển thị cho phía gọi lệnh. */
    @Column(name = "error_title")
    private String errorTitle;

    /** Thông tin chi tiết về lỗi xử lý lệnh. */
    @Column(name = "error_detail", columnDefinition = "text")
    private String errorDetail;

    /** Thời điểm lệnh được tạo. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Thời điểm lệnh được cập nhật gần nhất. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Thời điểm lệnh kết thúc xử lý. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
