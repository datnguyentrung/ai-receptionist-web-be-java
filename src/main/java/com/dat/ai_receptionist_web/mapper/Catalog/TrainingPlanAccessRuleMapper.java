package com.dat.ai_receptionist_web.mapper.Catalog;
import com.dat.ai_receptionist_web.domain.Catalog.TrainingPlanAccessRule;
import com.dat.ai_receptionist_web.dto.Catalog.TrainingPlanAccessRuleDTO;
import org.mapstruct.*;
@Mapper(componentModel="spring", uses={TrainingPlanMapper.class, CourseMapper.class})
public interface TrainingPlanAccessRuleMapper {
 @Mapping(target="targetCourseId",source="targetCourse.courseId") @Mapping(target="targetCourseName",source="targetCourse.name") @Mapping(target="trainingPlanId",source="trainingPlan.trainingPlanId") TrainingPlanAccessRuleDTO.SimpleResponse toSimpleResponse(TrainingPlanAccessRule entity);
 @Mapping(target="trainingPlan",source="trainingPlan") @Mapping(target="targetCourse",source="targetCourse") TrainingPlanAccessRuleDTO.Response toResponse(TrainingPlanAccessRule entity);
 @BeanMapping(ignoreByDefault=true) @Mapping(target="sessionType",source="sessionType") @Mapping(target="targetCourse",ignore=true) @Mapping(target="trainingPlan",ignore=true) void updateEntity(TrainingPlanAccessRuleDTO.CreateRequest request,@MappingTarget TrainingPlanAccessRule entity);
 @BeanMapping(ignoreByDefault=true) @Mapping(target="sessionType",source="sessionType") @Mapping(target="targetCourse",ignore=true) void updateEntity(TrainingPlanAccessRuleDTO.UpdateRequest request,@MappingTarget TrainingPlanAccessRule entity);
}
