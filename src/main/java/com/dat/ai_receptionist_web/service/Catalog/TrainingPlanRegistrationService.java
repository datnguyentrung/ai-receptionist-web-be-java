package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanRegistrationDTO;
import com.dat.ai_receptionist_web.enums.Catalog.TrainingPlanStatus;
import com.dat.ai_receptionist_web.enums.Core.ScheduleStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.repository.Catalog.CourseRepository;
import com.dat.ai_receptionist_web.repository.Catalog.CourseScheduleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanAccessRuleRepository;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanRepository;
import com.dat.ai_receptionist_web.repository.Catalog.TrainingPlanRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingPlanRegistrationService {
    private final TrainingPlanRepository planRepository;
    private final TrainingPlanRequirementRepository requirementRepository;
    private final TrainingPlanAccessRuleRepository accessRuleRepository;
    private final CourseScheduleRepository courseScheduleRepository;

    @Transactional(readOnly = true)
    public List<TrainingPlanRegistrationDTO.CatalogItem> catalog() {
        List<TrainingPlan> plans = planRepository.findByStatusOrderByNameAsc(TrainingPlanStatus.ACTIVE);
        if (plans.isEmpty()) return List.of();
        Set<UUID> planIds = plans.stream().map(TrainingPlan::getTrainingPlanId).collect(Collectors.toSet());
        Map<UUID, List<TrainingPlanRequirement>> requirementsByPlan = requirementRepository
                .findDetailedByTrainingPlanIdIn(planIds).stream()
                .collect(Collectors.groupingBy(r -> r.getTrainingPlan().getTrainingPlanId()));
        Map<UUID, List<com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAccessRule>> rulesByPlan = accessRuleRepository
                .findDetailedByTrainingPlanIdIn(planIds).stream()
                .collect(Collectors.groupingBy(r -> r.getTrainingPlan().getTrainingPlanId()));
        return plans.stream().map(plan -> catalogItem(plan,
                rulesByPlan.getOrDefault(plan.getTrainingPlanId(), List.of()),
                requirementsByPlan.getOrDefault(plan.getTrainingPlanId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public TrainingPlanRegistrationDTO.RegistrationOptions options(UUID planId, LocalDate startDate) {
        TrainingPlan plan = planRepository.findByTrainingPlanIdAndStatus(planId, TrainingPlanStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.TRAINING_PLAN_NOT_FOUND));

        LocalDate endDate = startDate.plusMonths(plan.getDurationMonths());
        List<TrainingPlanRequirement> requirements = requirementRepository.findByTrainingPlan_TrainingPlanId(planId);
        var rules = accessRuleRepository.findDetailedByTrainingPlanId(planId);
        Set<UUID> recurringCourseIds = rules
                .stream().map(rule -> rule.getTargetCourse().getCourseId()).collect(Collectors.toSet());
        List<CourseSchedule> schedules = recurringCourseIds.isEmpty() ? List.of() :
                courseScheduleRepository.findActiveOverlappingByCourseIds(recurringCourseIds, ScheduleStatus.ACTIVE, startDate, endDate);

        List<TrainingPlanRegistrationDTO.RecurringRequirement> recurring = requirements.stream()
                .map(r -> new TrainingPlanRegistrationDTO.RecurringRequirement(
                        r.getTrainingPlanRequirementId(), label(r), r.getSessionType(), r.getSessionsPerWeek(),
                        schedules.stream()
                                .filter(s -> supports(r, rules, s))
                                .map(s -> scheduleOption(s, r.getSessionType()))
                                .toList()))
                .toList();
        return new TrainingPlanRegistrationDTO.RegistrationOptions(planId, startDate, endDate, recurring);
    }

    private TrainingPlanRegistrationDTO.CatalogItem catalogItem(TrainingPlan plan,
                                                                  List<com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAccessRule> rules,
                                                                  List<TrainingPlanRequirement> planRequirements) {
        var requirements = planRequirements.stream()
                .map(r -> new TrainingPlanRegistrationDTO.Requirement(r.getTrainingPlanRequirementId(), r.getSessionType(),
                        r.getSessionsPerWeek())).toList();
        return new TrainingPlanRegistrationDTO.CatalogItem(plan.getTrainingPlanId(), plan.getName(), plan.getLevel(),
                plan.getDurationMonths(), plan.getIncludedSessionCount(), plan.getBasePrice(), plan.getFinalPrice(), requirements);
    }

    private boolean supports(TrainingPlanRequirement requirement,
                             List<com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAccessRule> rules,
                             CourseSchedule schedule) {
        return rules.stream().anyMatch(rule -> rule.getSessionType() == requirement.getSessionType()
                && rule.getTargetCourse().getCourseId().equals(schedule.getCourse().getCourseId()));
    }

    private TrainingPlanRegistrationDTO.ScheduleOption scheduleOption(CourseSchedule schedule,
                                                                       com.dat.ai_receptionist_web.enums.Catalog.SessionType sessionType) {
        var template = schedule.getClassSchedule();
        return new TrainingPlanRegistrationDTO.ScheduleOption(schedule.getCourseScheduleId(), schedule.getCourse().getCourseId(),
                schedule.getCourse().getName(), template.getBranch().getName(), template.getWeekday().name(),
                template.getStartTime(), template.getEndTime(), sessionType);
    }

    private String label(TrainingPlanRequirement requirement) {
        return requirement.getSessionType().name() + " · " + requirement.getSessionsPerWeek() + " buổi/tuần";
    }
}
