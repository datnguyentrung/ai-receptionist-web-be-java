package com.dat.ai_receptionist_web.mapper.Catalog;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanRequirement;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanRequirementDTO;
import org.mapstruct.*;
@Mapper(componentModel = "spring", uses = TrainingPlanMapper.class)
public interface TrainingPlanRequirementMapper {
 TrainingPlanRequirementDTO.BriefResponse toBriefResponse(TrainingPlanRequirement entity);
 @Mapping(target="trainingPlanId", source="trainingPlan.trainingPlanId") TrainingPlanRequirementDTO.SimpleResponse toSimpleResponse(TrainingPlanRequirement entity);
 @Mapping(target="trainingPlan", source="trainingPlan") TrainingPlanRequirementDTO.Response toResponse(TrainingPlanRequirement entity);
 @BeanMapping(ignoreByDefault=true) @Mapping(target="sessionType",source="sessionType") @Mapping(target="sessionsPerWeek",source="sessionsPerWeek") void updateEntity(TrainingPlanRequirementDTO.CreateRequest request,@MappingTarget TrainingPlanRequirement entity);
 @BeanMapping(ignoreByDefault=true) @Mapping(target="sessionType",source="sessionType") @Mapping(target="sessionsPerWeek",source="sessionsPerWeek") void updateEntity(TrainingPlanRequirementDTO.UpdateRequest request,@MappingTarget TrainingPlanRequirement entity);
}
