package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ClassScheduleMapper.class)
public interface CourseScheduleMapper {
    @Mapping(target = "courseId", source = "course.courseId")
    CourseScheduleDTO.Response toResponse(CourseSchedule entity);

    @Mapping(target = "courseId", source = "course.courseId")
    CourseScheduleDTO.SimpleResponse toSimpleResponse(CourseSchedule entity);
}
