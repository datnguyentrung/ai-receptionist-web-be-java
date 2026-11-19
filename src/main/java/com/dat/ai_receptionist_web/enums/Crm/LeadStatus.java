package com.dat.ai_receptionist_web.enums.Crm;

public enum LeadStatus {
    NEW,             // Người mới, chưa được liên hệ
    CONTACTED,       // Đã liên hệ/tư vấn lần đầu
    TRIAL_BOOKED,    // Đã đặt lịch học thử
    TRIAL_ATTENDED,  // Đã tham gia buổi học thử
    CONVERTED,       // Đã chuyển đổi thành học viên chính thức
    LOST             // Không tiếp tục / không chuyển đổi
}