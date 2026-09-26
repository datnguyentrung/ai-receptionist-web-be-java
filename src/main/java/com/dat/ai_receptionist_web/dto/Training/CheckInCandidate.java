package com.dat.ai_receptionist_web.dto.Training;

import com.dat.ai_receptionist_web.enums.Training.AssignmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CheckInCandidate(
        ContextType contextType,
        UUID classSessionId,
        UUID participationId,
        AssignmentType assignmentType,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime
) {
    public enum ContextType {
        STUDENT,
        STAFF
    }
}
