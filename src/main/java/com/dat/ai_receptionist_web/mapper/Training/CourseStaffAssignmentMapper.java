package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.CourseStaffAssignment;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, CourseMapper.class})
public interface CourseStaffAssignmentMapper {
    CourseStaffAssignmentDTO.Response toResponse(CourseStaffAssignment entity);

    CourseStaffAssignmentDTO.SimpleResponse toSimpleResponse(CourseStaffAssignment entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "staffPerson", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "assignmentType", source = "assignmentType")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "assignmentStatus", source = "assignmentStatus")
    @Mapping(target = "note", source = "note")
    void updateEntity(CourseStaffAssignmentDTO.UpdateRequest request, @MappingTarget CourseStaffAssignment entity);
}
