package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Training.ClassSessionDTO;
import com.dat.ai_receptionist_web.mapper.Catalog.CourseMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = CourseMapper.class)
public interface ClassSessionMapper {
    ClassSessionDTO.Response toResponse(ClassSession entity);

    ClassSessionDTO.SimpleResponse toSimpleResponse(ClassSession entity);

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
