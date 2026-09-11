package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CoachTimesheetMapper {
    @Mapping(target = "courseStaffAssignmentId", source = "courseStaffAssignment.courseStaffAssignmentId")
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    @Mapping(target = "allowedActions", expression = "java(com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO.AllowedActions.none())")
    CoachTimesheetDTO.Response toResponse(CoachTimesheet entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "checkOutTime", source = "checkOutTime")
    @Mapping(target = "note", source = "note")
    void updateEntity(CoachTimesheetDTO.UpdateRequest request, @MappingTarget CoachTimesheet entity);
}
