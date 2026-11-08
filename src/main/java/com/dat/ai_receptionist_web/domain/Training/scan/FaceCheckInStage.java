package com.dat.ai_receptionist_web.domain.Training.scan;

public enum FaceCheckInStage {
    READY,
    IMAGE_RECEIVED,
    FACE_DETECTED,
    EMBEDDING_READY,
    MATCH_FOUND,
    ELIGIBILITY_CHECKED,
    ATTENDANCE_SAVED,
    SUCCEEDED,
    REJECTED,
    FAILED,
    EXPIRED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == REJECTED || this == FAILED || this == EXPIRED;
    }
}
