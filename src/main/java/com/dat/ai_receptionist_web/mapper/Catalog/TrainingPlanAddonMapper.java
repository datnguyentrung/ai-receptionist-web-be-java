package com.dat.ai_receptionist_web.mapper.Catalog;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAddon;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanAddonDTO;
import org.mapstruct.*;
@Mapper(componentModel="spring", uses=TrainingPlanMapper.class)
public interface TrainingPlanAddonMapper {
 @Mapping(target="trainingPlanId",source="trainingPlan.trainingPlanId") TrainingPlanAddonDTO.SimpleResponse toSimpleResponse(TrainingPlanAddon entity);
 @Mapping(target="trainingPlan",source="trainingPlan") TrainingPlanAddonDTO.Response toResponse(TrainingPlanAddon entity);
 @BeanMapping(ignoreByDefault=true) @Mapping(target="name",source="name") @Mapping(target="extraSessionsPerWeek",source="extraSessionsPerWeek") @Mapping(target="price",source="price") @Mapping(target="status",source="status") @Mapping(target="trainingPlan",ignore=true) void updateEntity(TrainingPlanAddonDTO.CreateRequest request,@MappingTarget TrainingPlanAddon entity);
 @BeanMapping(ignoreByDefault=true) @Mapping(target="name",source="name") @Mapping(target="extraSessionsPerWeek",source="extraSessionsPerWeek") @Mapping(target="price",source="price") @Mapping(target="status",source="status") void updateEntity(TrainingPlanAddonDTO.UpdateRequest request,@MappingTarget TrainingPlanAddon entity);
}
