package com.dat.ai_receptionist_web.enums.Training;

import lombok.Getter;

@Getter
public enum MovementType {
    NEW,                    // Học viên mới
    REACTIVATED,            // Học viên quay lại
    LEAVE;                  // Học viên nghỉ

    private final String label;

    MovementType() {
        this.label = this.name();
    }
}