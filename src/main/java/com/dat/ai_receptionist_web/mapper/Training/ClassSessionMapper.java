package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CourseMapper.class, PersonMapper.class})
public interface ClassSessionMapper {
    ClassSessionDTO.Response toResponse(ClassSession entity);

    @Mapping(target = "primaryCoach", ignore = true)
    ClassSessionDTO.SimpleResponse toSimpleResponse(ClassSession entity);

    @Mapping(target = "primaryCoach", source = "primaryCoach")
    @Mapping(target = "classSessionId", source = "entity.classSessionId")
    @Mapping(target = "course", source = "entity.course")
    @Mapping(target = "sessionDate", source = "entity.sessionDate")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "attendanceClosed", source = "entity.attendanceClosed")
    @Mapping(target = "startTime", source = "entity.startTime")
    @Mapping(target = "endTime", source = "entity.endTime")
    ClassSessionDTO.SimpleResponse toSimpleResponse(ClassSession entity, Person primaryCoach);

    @Mapping(target = "courseId", source = "course.courseId")
    @Mapping(target = "courseName", source = "course.name")
    @Mapping(target = "primaryCoach", ignore = true)
    ClassSessionDTO.CalendarResponse toCalendarResponse(ClassSession entity);

    @Mapping(target = "courseId", source = "entity.course.courseId")
    @Mapping(target = "courseName", source = "entity.course.name")
    @Mapping(target = "primaryCoach", source = "primaryCoach")
    @Mapping(target = "classSessionId", source = "entity.classSessionId")
    @Mapping(target = "sessionDate", source = "entity.sessionDate")
    @Mapping(target = "startTime", source = "entity.startTime")
    @Mapping(target = "endTime", source = "entity.endTime")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "attendanceClosed", source = "entity.attendanceClosed")
    ClassSessionDTO.CalendarResponse toCalendarResponse(ClassSession entity, Person primaryCoach);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "sessionDate", source = "sessionDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "attendanceClosed", ignore = true)
    @Mapping(target = "attendanceReopenedUntil", ignore = true)
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "note", source = "note")
    void updateEntity(ClassSessionDTO.UpdateRequest request, @MappingTarget ClassSession entity);
}
