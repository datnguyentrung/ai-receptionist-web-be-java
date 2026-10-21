package com.dat.ai_receptionist_web.dto.Catalog;

import com.dat.ai_receptionist_web.enums.Catalog.FulfillmentMode;
import com.dat.ai_receptionist_web.enums.Catalog.SessionType;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Read models deliberately shaped for the registration flow. */
public final class TrainingPlanRegistrationDTO {
    private TrainingPlanRegistrationDTO() {}

    public record CourseOption(UUID courseId, String name, Integer capacity) {}
    public record Requirement(UUID requirementId, SessionType sessionType,
                              FulfillmentMode fulfillmentMode, int sessionsPerWeek,
                              boolean homeCourseOnly) {}
    public record CatalogItem(UUID trainingPlanId, String name, ScheduleLevel level,
                              int durationMonths, int includedSessionCount,
                              BigDecimal basePrice, BigDecimal finalPrice,
                              List<CourseOption> homeCourses, List<Requirement> requirements) {}
    public record ScheduleOption(UUID courseScheduleId, UUID courseId, String courseName,
                                 String branchName, String dayOfWeek,
                                 LocalTime startTime, LocalTime endTime,
                                 SessionType sessionType) {}
    public record RecurringRequirement(UUID requirementId, String label, SessionType sessionType,
                                       int sessionsPerWeek, boolean homeCourseOnly,
                                       List<ScheduleOption> options) {}
    public record FlexRequirement(UUID requirementId, SessionType sessionType,
                                  int sessionsPerWeek, String description) {}
    public record RegistrationOptions(UUID trainingPlanId, UUID homeCourseId,
                                      LocalDate startDate, LocalDate endDate,
                                      List<RecurringRequirement> recurringRequirements,
                                      List<FlexRequirement> flexRequirements) {}
}
