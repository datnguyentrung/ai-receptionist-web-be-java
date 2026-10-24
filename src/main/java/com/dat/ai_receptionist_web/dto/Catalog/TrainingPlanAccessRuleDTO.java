package com.dat.ai_receptionist_web.dto.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.*;
import jakarta.validation.constraints.*;

import java.util.UUID;

public final class TrainingPlanAccessRuleDTO {
    private TrainingPlanAccessRuleDTO() {
    }

    public record CreateRequest(
            @NotNull
            UUID trainingPlanId,
            @NotNull
            UUID targetCourseId,
            @NotNull
            SessionType sessionType
    ) {
    }

    public record UpdateRequest(
            @NotNull
            UUID targetCourseId,
            @NotNull
            SessionType sessionType
    ) {
    }

    public record BriefResponse(
            UUID trainingPlanAccessRuleId,
            UUID targetCourseId,
            SessionType sessionType
    ) {
    }

    public record SimpleResponse(
            UUID trainingPlanAccessRuleId,
            UUID trainingPlanId,
            UUID targetCourseId,
            String targetCourseName,
            SessionType sessionType
    ) {
    }

    public record Response(
            UUID trainingPlanAccessRuleId,
            TrainingPlanDTO.BriefResponse trainingPlan,
            CourseDTO.SimpleResponse targetCourse,
            SessionType sessionType
    ) {
    }
}
