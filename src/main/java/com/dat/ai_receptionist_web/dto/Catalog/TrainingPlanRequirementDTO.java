package com.dat.ai_receptionist_web.dto.Catalog;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class TrainingPlanRequirementDTO {
 private TrainingPlanRequirementDTO() {}
 public record CreateRequest(@NotNull UUID trainingPlanId, @NotNull SessionType sessionType, @Positive int sessionsPerWeek) {}
 public record UpdateRequest(@NotNull SessionType sessionType, @Positive int sessionsPerWeek) {}
 public record BriefResponse(UUID trainingPlanRequirementId, SessionType sessionType, int sessionsPerWeek) {}
 public record SimpleResponse(UUID trainingPlanRequirementId, UUID trainingPlanId, SessionType sessionType, int sessionsPerWeek) {}
 public record Response(UUID trainingPlanRequirementId, TrainingPlanDTO.BriefResponse trainingPlan, SessionType sessionType, int sessionsPerWeek) {}
}
