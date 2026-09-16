package com.dat.ai_receptionist_web.dto.Skill;

import jakarta.validation.constraints.*;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;

public final class FitnessDTO {
    private FitnessDTO() {
    }

    public record CreateRequest(@NotNull ScheduleLevel scheduleLevel, int amount, int duration) {
    }

    public record UpdateRequest(@NotNull ScheduleLevel scheduleLevel, int amount, int duration) {
    }

    public record Response(Long fitnessId, ScheduleLevel scheduleLevel, int amount, int duration) {
    }

    public record SimpleResponse(Long fitnessId, ScheduleLevel scheduleLevel, int amount, int duration) {
    }
}
