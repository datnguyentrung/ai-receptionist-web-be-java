package com.dat.ai_receptionist_web.dto.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.TrainingPlanStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class TrainingPlanDTO {
    private TrainingPlanDTO() {
    }

    public record CreateRequest(
            @NotBlank
            String name,
            @NotNull
            ScheduleLevel level,
            @Positive
            int durationMonths,
            @Positive
            int includedSessionCount,
            @NotNull
            @PositiveOrZero
            BigDecimal basePrice,
            @NotNull
            @PositiveOrZero
            BigDecimal finalPrice,
            @NotNull
            TrainingPlanStatus status
    ) {
    }

    public record UpdateRequest(
            @NotBlank
            String name,
            @NotNull
            ScheduleLevel level,
            @Positive
            int durationMonths,
            @Positive
            int includedSessionCount,
            @NotNull
            @PositiveOrZero
            BigDecimal basePrice,
            @NotNull
            @PositiveOrZero
            BigDecimal finalPrice,
            @NotNull
            TrainingPlanStatus status
    ) {
    }

    public record BriefResponse(
            UUID trainingPlanId,
            String name,
            ScheduleLevel level,
            TrainingPlanStatus status
    ) {
    }

    public record SimpleResponse(
            UUID trainingPlanId,
            String name,
            ScheduleLevel level,
            int durationMonths,
            int includedSessionCount,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            TrainingPlanStatus status
    ) {
    }

    public record Response(
            UUID trainingPlanId,
            String name,
            ScheduleLevel level,
            int durationMonths,
            int includedSessionCount,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            TrainingPlanStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<TrainingPlanRequirementDTO.SimpleResponse> requirements,
            List<TrainingPlanAccessRuleDTO.SimpleResponse> accessRules,
            List<TrainingPlanAddonDTO.SimpleResponse> addons
    ) {
    }
}
