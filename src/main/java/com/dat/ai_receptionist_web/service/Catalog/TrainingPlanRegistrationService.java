package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanRegistrationDTO;
import com.dat.ai_receptionist_web.enums.Catalog.PlanAccessMode;
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
    private final CourseRepository courseRepository;
    private final CourseScheduleRepository courseScheduleRepository;

    @Transactional(readOnly = true)
    public List<TrainingPlanRegistrationDTO.CatalogItem> catalog() {
        return planRepository.findByStatusOrderByNameAsc(TrainingPlanStatus.ACTIVE).stream()
                .map(this::catalogItem).toList();
    }

    @Transactional(readOnly = true)
    public TrainingPlanRegistrationDTO.RegistrationOptions options(UUID planId, UUID homeCourseId, LocalDate startDate) {
        TrainingPlan plan = planRepository.findByTrainingPlanIdAndStatus(planId, TrainingPlanStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.TRAINING_PLAN_NOT_FOUND));
        var home = courseRepository.findById(homeCourseId)
                .orElseThrow(() -> new ApiException(CatalogErrorCode.COURSE_NOT_FOUND));
        boolean allowedHome = accessRuleRepository.existsByTrainingPlan_TrainingPlanIdAndTargetCourse_CourseIdAndAccessMode(
                planId, homeCourseId, PlanAccessMode.HOME);
        if (!allowedHome) throw new ApiException(CatalogErrorCode.TRAINING_PLAN_HOME_COURSE_NOT_ALLOWED);

        LocalDate endDate = startDate.plusMonths(plan.getDurationMonths());
        List<TrainingPlanRequirement> requirements = requirementRepository.findByTrainingPlan_TrainingPlanId(planId);
        Set<UUID> recurringCourseIds = accessRuleRepository.findDetailedByTrainingPlanIdAndAccessMode(planId, PlanAccessMode.RECURRING)
                .stream().map(rule -> rule.getTargetCourse().getCourseId()).collect(Collectors.toSet());
        List<CourseSchedule> schedules = recurringCourseIds.isEmpty() ? List.of() :
                courseScheduleRepository.findActiveOverlappingByCourseIds(recurringCourseIds, ScheduleStatus.ACTIVE, startDate, endDate);

        List<TrainingPlanRegistrationDTO.RecurringRequirement> recurring = requirements.stream()
                .map(r -> new TrainingPlanRegistrationDTO.RecurringRequirement(
                        r.getTrainingPlanRequirementId(), label(r), r.getSessionType(), r.getSessionsPerWeek(),
                        schedules.stream().filter(s -> supports(r, homeCourseId, s)).map(this::scheduleOption).toList()))
                .toList();
        return new TrainingPlanRegistrationDTO.RegistrationOptions(planId, homeCourseId, startDate, endDate, recurring);
    }

    private TrainingPlanRegistrationDTO.CatalogItem catalogItem(TrainingPlan plan) {
        var homeCourses = accessRuleRepository.findDetailedByTrainingPlanIdAndAccessMode(plan.getTrainingPlanId(), PlanAccessMode.HOME).stream()
                .map(rule -> rule.getTargetCourse()).distinct()
                .map(course -> new TrainingPlanRegistrationDTO.CourseOption(course.getCourseId(), course.getName(), course.getCapacity())).toList();
        var requirements = requirementRepository.findByTrainingPlan_TrainingPlanId(plan.getTrainingPlanId()).stream()
                .map(r -> new TrainingPlanRegistrationDTO.Requirement(r.getTrainingPlanRequirementId(), r.getSessionType(),
                        r.getSessionsPerWeek())).toList();
        return new TrainingPlanRegistrationDTO.CatalogItem(plan.getTrainingPlanId(), plan.getName(), plan.getLevel(),
                plan.getDurationMonths(), plan.getIncludedSessionCount(), plan.getBasePrice(), plan.getFinalPrice(), homeCourses, requirements);
    }

    private boolean supports(TrainingPlanRequirement requirement, UUID homeCourseId, CourseSchedule schedule) {
        return schedule.getSessionType() == requirement.getSessionType()
                && schedule.getCourse().getCourseId().equals(homeCourseId);
    }

    private TrainingPlanRegistrationDTO.ScheduleOption scheduleOption(CourseSchedule schedule) {
        var template = schedule.getClassSchedule();
        return new TrainingPlanRegistrationDTO.ScheduleOption(schedule.getCourseScheduleId(), schedule.getCourse().getCourseId(),
                schedule.getCourse().getName(), template.getBranch().getName(), template.getWeekday().name(),
                template.getStartTime(), template.getEndTime(), schedule.getSessionType());
    }

    private String label(TrainingPlanRequirement requirement) {
        return requirement.getSessionType().name() + " · " + requirement.getSessionsPerWeek() + " buổi/tuần";
    }
}
