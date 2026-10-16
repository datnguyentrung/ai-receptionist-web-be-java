package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import com.dat.ai_receptionist_web.dto.Training.CourseStaffAssignmentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = ClassScheduleMapper.class)
public interface CourseScheduleMapper {
    @Mapping(target = "courseId", source = "entity.course.courseId")
    @Mapping(target = "staffAssignments", source = "staffAssignments")
    CourseScheduleDTO.Response toResponse(
            CourseSchedule entity,
            List<CourseStaffAssignmentDTO.BriefResponse> staffAssignments
    );

    default CourseScheduleDTO.Response toResponse(CourseSchedule entity) {
        return toResponse(entity, List.of());
    }

    @Mapping(target = "courseId", source = "course.courseId")
    CourseScheduleDTO.SimpleResponse toSimpleResponse(CourseSchedule entity);
}
