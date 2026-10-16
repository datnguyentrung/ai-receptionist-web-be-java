package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "courseSchedules", ignore = true)
    @Mapping(target = "currentStudentCount", constant = "0")
    CourseDTO.Response toResponse(Course entity);

    @Mapping(target = "courseSchedules", ignore = true)
    @Mapping(target = "courseId", source = "entity.courseId")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "level", source = "entity.level")
    @Mapping(target = "capacity", source = "entity.capacity")
    @Mapping(target = "currentStudentCount", source = "currentStudentCount")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "classSessionGeneratedUntil", source = "entity.classSessionGeneratedUntil")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "updatedAt", source = "entity.updatedAt")
    CourseDTO.Response toResponse(
            Course entity,
            int currentStudentCount
    );

    @Mapping(target = "courseSchedules", ignore = true)
    @Mapping(target = "currentStudentCount", constant = "0")
    CourseDTO.SimpleResponse toSimpleResponse(Course entity);

    @Mapping(target = "courseSchedules", ignore = true)
    @Mapping(target = "courseId", source = "entity.courseId")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "level", source = "entity.level")
    @Mapping(target = "capacity", source = "entity.capacity")
    @Mapping(target = "currentStudentCount", source = "currentStudentCount")
    @Mapping(target = "status", source = "entity.status")
    CourseDTO.SimpleResponse toSimpleResponse(Course entity, int currentStudentCount);

    default CourseDTO.Response withSchedules(CourseDTO.Response base, List<com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.Response> schedules) {
        return new CourseDTO.Response(base.courseId(), schedules, base.name(), base.level(), base.capacity(),
                base.currentStudentCount(), base.status(), base.classSessionGeneratedUntil(),
                base.createdAt(), base.updatedAt());
    }

    default CourseDTO.SimpleResponse withSchedules(CourseDTO.SimpleResponse base, List<com.dat.ai_receptionist_web.dto.Catalog.CourseScheduleDTO.SimpleResponse> schedules) {
        return new CourseDTO.SimpleResponse(base.courseId(), schedules, base.name(), base.level(), base.capacity(),
                base.currentStudentCount(), base.status());
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "capacity", source = "capacity")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "status", source = "status")
    void updateEntity(CourseDTO.UpdateRequest request, @MappingTarget Course entity);
}
