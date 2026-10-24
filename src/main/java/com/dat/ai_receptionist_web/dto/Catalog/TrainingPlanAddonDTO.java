package com.dat.ai_receptionist_web.dto.Catalog;
import com.dat.ai_receptionist_web.enums.Catalog.AddonStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public final class TrainingPlanAddonDTO {
 private TrainingPlanAddonDTO() {}
 public record CreateRequest(@NotNull UUID trainingPlanId, @NotBlank String name, @Positive int extraSessionsPerWeek, @NotNull @PositiveOrZero BigDecimal price, @NotNull AddonStatus status) {}
 public record UpdateRequest(@NotBlank String name, @Positive int extraSessionsPerWeek, @NotNull @PositiveOrZero BigDecimal price, @NotNull AddonStatus status) {}
 public record BriefResponse(UUID trainingPlanAddonId, String name, int extraSessionsPerWeek, BigDecimal price, AddonStatus status) {}
 public record SimpleResponse(UUID trainingPlanAddonId, UUID trainingPlanId, String name, int extraSessionsPerWeek, BigDecimal price, AddonStatus status) {}
 public record Response(UUID trainingPlanAddonId, TrainingPlanDTO.BriefResponse trainingPlan, String name, int extraSessionsPerWeek, BigDecimal price, AddonStatus status) {}
}
