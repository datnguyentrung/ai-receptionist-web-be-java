package com.dat.ai_receptionist_web.service.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.*;
import com.dat.ai_receptionist_web.dto.*;
import com.dat.ai_receptionist_web.dto.Catalog.*;
import com.dat.ai_receptionist_web.enums.Catalog.*;
import com.dat.ai_receptionist_web.enums.Core.ScheduleLevel;
import com.dat.ai_receptionist_web.error.*;
import com.dat.ai_receptionist_web.error.code.CatalogErrorCode;
import com.dat.ai_receptionist_web.mapper.Catalog.*;
import com.dat.ai_receptionist_web.repository.Catalog.*;
import com.dat.ai_receptionist_web.repository.Finance.CoursePurchaseRepository;
import com.dat.ai_receptionist_web.repository.Finance.EnrollmentAddonPurchaseRepository;
import com.dat.ai_receptionist_web.repository.Training.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class TrainingPlanService {
 private final TrainingPlanRepository plans; private final TrainingPlanRequirementRepository requirements; private final TrainingPlanAccessRuleRepository rules; private final TrainingPlanAddonRepository addons;
 private final CoursePurchaseRepository purchases; private final EnrollmentAddonPurchaseRepository addonPurchases; private final StudentEnrollmentScheduleRepository enrollmentSchedules; private final StudentSessionBookingRepository bookings; private final TrainingPlanMapper mapper; private final TrainingPlanRequirementMapper requirementMapper; private final TrainingPlanAccessRuleMapper ruleMapper; private final TrainingPlanAddonMapper addonMapper;
 @Transactional(readOnly=true) public PageResponse<TrainingPlanDTO.SimpleResponse> list(TrainingPlanStatus status, ScheduleLevel level, Pageable pageable) { return PageResponse.of(plans.findAllFiltered(status, level, pageable), mapper::toSimpleResponse); }
 @Transactional(readOnly=true) public TrainingPlanDTO.Response get(UUID id) { return detail(find(id)); }
 @Transactional public TrainingPlanDTO.Response create(TrainingPlanDTO.CreateRequest request) { TrainingPlan entity=new TrainingPlan(); mapper.updateEntity(request,entity); return detail(plans.save(entity)); }
 @Transactional public TrainingPlanDTO.Response update(UUID id, TrainingPlanDTO.UpdateRequest request) { TrainingPlan entity=find(id); if (sold(id) && structuralChanged(entity,request)) throw new ApiException(CatalogErrorCode.TRAINING_PLAN_CONFIGURATION_LOCKED); mapper.updateEntity(request,entity); return detail(entity); }
 @Transactional public void delete(UUID id) { TrainingPlan plan=find(id); if(sold(id)) throw new ApiException(CatalogErrorCode.TRAINING_PLAN_IN_USE); var reqs=requirements.findByTrainingPlan_TrainingPlanId(id); var as=addons.findByTrainingPlan_TrainingPlanId(id); if(reqs.stream().anyMatch(r -> enrollmentSchedules.existsByRequirement_TrainingPlanRequirementId(r.getTrainingPlanRequirementId()) || bookings.existsByRequirement_TrainingPlanRequirementId(r.getTrainingPlanRequirementId())) || as.stream().anyMatch(a -> addonPurchases.existsByAddon_TrainingPlanAddonId(a.getTrainingPlanAddonId()))) throw new ApiException(CatalogErrorCode.TRAINING_PLAN_IN_USE); rules.deleteAll(rules.findByTrainingPlan_TrainingPlanId(id)); requirements.deleteAll(reqs); addons.deleteAll(as); plans.delete(plan); }
 TrainingPlan find(UUID id) { return plans.findById(id).orElseThrow(() -> new ApiException(CatalogErrorCode.TRAINING_PLAN_NOT_FOUND)); }
 boolean sold(UUID planId) { return purchases.existsByTrainingPlan_TrainingPlanId(planId); }
 private TrainingPlanDTO.Response detail(TrainingPlan p) { var b=mapper.toResponse(p); return new TrainingPlanDTO.Response(b.trainingPlanId(),b.name(),b.level(),b.durationMonths(),b.includedSessionCount(),b.basePrice(),b.finalPrice(),b.status(),b.createdAt(),b.updatedAt(),requirements.findByTrainingPlan_TrainingPlanId(p.getTrainingPlanId()).stream().map(requirementMapper::toSimpleResponse).toList(),rules.findByTrainingPlan_TrainingPlanId(p.getTrainingPlanId()).stream().map(ruleMapper::toSimpleResponse).toList(),addons.findByTrainingPlan_TrainingPlanId(p.getTrainingPlanId()).stream().map(addonMapper::toSimpleResponse).toList()); }
 private boolean structuralChanged(TrainingPlan p, TrainingPlanDTO.UpdateRequest r) { return p.getLevel()!=r.level() || p.getDurationMonths()!=r.durationMonths() || p.getIncludedSessionCount()!=r.includedSessionCount() || p.getBasePrice().compareTo(r.basePrice())!=0 || p.getFinalPrice().compareTo(r.finalPrice())!=0; }
}
