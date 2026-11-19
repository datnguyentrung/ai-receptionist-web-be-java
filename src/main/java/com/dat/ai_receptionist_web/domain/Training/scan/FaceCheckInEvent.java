package com.dat.ai_receptionist_web.domain.Training.scan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Từng sự kiện trong quá trình xử lý một lượt điểm danh bằng khuôn mặt.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "face_check_in_event", schema = "training")
public class FaceCheckInEvent {
    /** Mã định danh duy nhất của sự kiện điểm danh khuôn mặt. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    /** Mã yêu cầu của lượt điểm danh khuôn mặt mà sự kiện thuộc về. */
    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    /** Giai đoạn xử lý khuôn mặt đã đạt được. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 32)
    private FaceCheckInStage stage;

    /** Kết quả xử lý của sự kiện, thường được lưu dưới dạng JSON. */
    @Column(name = "result_payload", columnDefinition = "text")
    private String resultPayload;

    /** Mã lỗi nếu giai đoạn xử lý thất bại. */
    @Column(name = "error_code", length = 128)
    private String errorCode;

    /** Tiêu đề lỗi hiển thị cho phía gọi điểm danh. */
    @Column(name = "error_title")
    private String errorTitle;

    /** Thông tin chi tiết về lỗi xử lý khuôn mặt. */
    @Column(name = "error_detail", columnDefinition = "text")
    private String errorDetail;

    /** Thời điểm sự kiện xử lý khuôn mặt thực tế xảy ra. */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
