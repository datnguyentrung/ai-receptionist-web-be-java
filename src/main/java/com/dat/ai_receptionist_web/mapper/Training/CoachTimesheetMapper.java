package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CoachTimesheet;
import com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CourseStaffAssignmentMapper.class, ClassSessionMapper.class})
public interface CoachTimesheetMapper {
    @Mapping(target = "allowedActions", expression = "java(com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO.AllowedActions.none())")
    CoachTimesheetDTO.Response toResponse(CoachTimesheet entity);

    @Mapping(target = "allowedActions", expression = "java(com.dat.ai_receptionist_web.dto.Training.CoachTimesheetDTO.AllowedActions.none())")
    CoachTimesheetDTO.SimpleResponse toSimpleResponse(CoachTimesheet entity);

    default CoachTimesheetDTO.Response toResponse(
            CoachTimesheet entity,
            CoachTimesheetDTO.AllowedActions allowedActions
    ) {
        CoachTimesheetDTO.Response base = toResponse(entity);
        return new CoachTimesheetDTO.Response(
                base.coachTimesheetId(),
                base.courseStaffAssignment(),
                base.classSession(),
                base.checkInTime(),
                base.checkOutTime(),
                base.note(),
                allowedActions,
                base.createdAt(),
                base.updatedAt()
        );
    }

    default CoachTimesheetDTO.SimpleResponse toSimpleResponse(
            CoachTimesheet entity,
            CoachTimesheetDTO.AllowedActions allowedActions
    ) {
        CoachTimesheetDTO.SimpleResponse base = toSimpleResponse(entity);
        return new CoachTimesheetDTO.SimpleResponse(
                base.coachTimesheetId(),
                base.courseStaffAssignment(),
                base.classSession(),
                base.checkInTime(),
                base.checkOutTime(),
                base.note(),
                allowedActions
        );
    }

    default CoachTimesheetDTO.AllowedActions toAllowedActions(boolean update, boolean delete) {
        return new CoachTimesheetDTO.AllowedActions(update, delete);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkInTime", source = "checkInTime")
    @Mapping(target = "checkOutTime", source = "checkOutTime")
    @Mapping(target = "note", source = "note")
    void updateEntity(CoachTimesheetDTO.UpdateRequest request, @MappingTarget CoachTimesheet entity);
}
