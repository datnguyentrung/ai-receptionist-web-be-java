package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = ClassScheduleMapper.class)
public interface CourseMapper {
    CourseDTO.Response toResponse(Course entity);

    CourseDTO.SimpleResponse toSimpleResponse(Course entity);

    default CourseDTO.CourseScheduleChangeResponse toScheduleChangeResponse(
            Course course,
            List<UUID> cancelledSessionIds,
            List<UUID> generatedSessionIds
    ) {
        return new CourseDTO.CourseScheduleChangeResponse(
                toResponse(course),
                cancelledSessionIds,
                generatedSessionIds
        );
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "classSchedule", ignore = true)
    @Mapping(target = "capacity", source = "capacity")
    @Mapping(target = "status", source = "status")
    void updateEntity(CourseDTO.UpdateRequest request, @MappingTarget Course entity);
}
