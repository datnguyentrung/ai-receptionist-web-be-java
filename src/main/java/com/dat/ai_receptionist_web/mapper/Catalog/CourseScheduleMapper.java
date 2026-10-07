package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.CourseSchedule;
import com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ClassScheduleMapper.class)
public interface CourseScheduleMapper {
    CourseScheduleDTO.Response toResponse(CourseSchedule entity);
    CourseScheduleDTO.SimpleResponse toSimpleResponse(CourseSchedule entity);
}
