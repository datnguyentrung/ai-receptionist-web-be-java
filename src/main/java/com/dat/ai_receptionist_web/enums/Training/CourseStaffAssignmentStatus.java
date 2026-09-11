package com.dat.ai_receptionist_web.enums.Training;

public enum CourseStaffAssignmentStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    ENDED,
    CANCELLED;

    public boolean isActiveLike() {
        return this == ACTIVE;
    }

    public boolean blocksNewAssignment() {
        return this == ACTIVE || this == PENDING || this == SUSPENDED;
    }

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * Historical policy access is valid only for assignments that were actually effective.
     * PENDING/SUSPENDED/CANCELLED must not grant course data access.
     */
    public boolean allowsPolicyAccess() {
        return this == ACTIVE || this == ENDED;
    }
}
