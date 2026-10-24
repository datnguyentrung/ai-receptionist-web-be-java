package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlan;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TrainingPlanMapper {
    TrainingPlanDTO.BriefResponse toBriefResponse(TrainingPlan entity);
    TrainingPlanDTO.SimpleResponse toSimpleResponse(TrainingPlan entity);
    @Mapping(target = "requirements", ignore = true) @Mapping(target = "accessRules", ignore = true) @Mapping(target = "addons", ignore = true)
    TrainingPlanDTO.Response toResponse(TrainingPlan entity);
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name") @Mapping(target = "level", source = "level") @Mapping(target = "durationMonths", source = "durationMonths")
    @Mapping(target = "includedSessionCount", source = "includedSessionCount") @Mapping(target = "basePrice", source = "basePrice") @Mapping(target = "finalPrice", source = "finalPrice") @Mapping(target = "status", source = "status")
    void updateEntity(TrainingPlanDTO.CreateRequest request, @MappingTarget TrainingPlan entity);
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name") @Mapping(target = "level", source = "level") @Mapping(target = "durationMonths", source = "durationMonths")
    @Mapping(target = "includedSessionCount", source = "includedSessionCount") @Mapping(target = "basePrice", source = "basePrice") @Mapping(target = "finalPrice", source = "finalPrice") @Mapping(target = "status", source = "status")
    void updateEntity(TrainingPlanDTO.UpdateRequest request, @MappingTarget TrainingPlan entity);
}
