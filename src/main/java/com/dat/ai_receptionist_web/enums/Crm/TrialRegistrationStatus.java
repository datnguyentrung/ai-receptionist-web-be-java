package com.dat.ai_receptionist_web.enums.Crm;

/**
 * Các trạng thái trong vòng đời của một lượt đăng ký học thử.
 */
public enum TrialRegistrationStatus {
    /**
     * Đã đặt lịch học thử nhưng chưa diễn ra.
     */
    BOOKED,

    /**
     * Khách hàng đã tham gia buổi học thử.
     */
    ATTENDED,

    /**
     * Khách hàng không đến buổi học thử.
     */
    NO_SHOW,

    /**
     * Lượt đăng ký học thử đã bị hủy.
     */
    CANCELLED
}
