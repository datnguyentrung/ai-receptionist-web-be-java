package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.Course;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Catalog.CourseDTO;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", uses = {ClassScheduleMapper.class, PersonMapper.class})
public interface CourseMapper {
    @Mapping(target = "primaryCoach", ignore = true)
    @Mapping(target = "assistantCoaches", expression = "java(java.util.List.of())")
    @Mapping(target = "teachingAssistants", expression = "java(java.util.List.of())")
    @Mapping(target = "manager", ignore = true)
    CourseDTO.Response toResponse(Course entity);

    @Mapping(target = "primaryCoach", source = "primaryCoach")
    @Mapping(target = "assistantCoaches", source = "assistantCoaches")
    @Mapping(target = "teachingAssistants", source = "teachingAssistants")
    @Mapping(target = "manager", source = "manager")
    @Mapping(target = "courseId", source = "entity.courseId")
    @Mapping(target = "classSchedule", source = "entity.classSchedule")
    @Mapping(target = "nextClassSchedule", source = "entity.nextClassSchedule")
    @Mapping(target = "nextScheduleEffectiveFrom", source = "entity.nextScheduleEffectiveFrom")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "capacity", source = "entity.capacity")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "classSessionGeneratedUntil", source = "entity.classSessionGeneratedUntil")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "updatedAt", source = "entity.updatedAt")
    CourseDTO.Response toResponse(
            Course entity,
            Person primaryCoach,
            List<Person> assistantCoaches,
            List<Person> teachingAssistants,
            Person manager
    );

    @Mapping(target = "primaryCoach", ignore = true)
    CourseDTO.SimpleResponse toSimpleResponse(Course entity);

    @Mapping(target = "primaryCoach", source = "primaryCoach")
    @Mapping(target = "courseId", source = "entity.courseId")
    @Mapping(target = "classSchedule", source = "entity.classSchedule")
    @Mapping(target = "nextClassSchedule", source = "entity.nextClassSchedule")
    @Mapping(target = "nextScheduleEffectiveFrom", source = "entity.nextScheduleEffectiveFrom")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "capacity", source = "entity.capacity")
    @Mapping(target = "status", source = "entity.status")
    CourseDTO.SimpleResponse toSimpleResponse(Course entity, Person primaryCoach);

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
