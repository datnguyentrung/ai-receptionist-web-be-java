package com.dat.ai_receptionist_web.mapper.Catalog;

import com.dat.ai_receptionist_web.domain.Catalog.ClassSchedule;
import com.dat.ai_receptionist_web.mapper.Core.BranchMapper;
import com.dat.ai_receptionist_web.dto.Catalog.ClassScheduleDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = BranchMapper.class)
public interface ClassScheduleMapper {
    ClassScheduleDTO.Response toResponse(ClassSchedule entity);

    ClassScheduleDTO.SimpleResponse toSimpleResponse(ClassSchedule entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "weekday", source = "weekday")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    void updateEntity(ClassScheduleDTO.UpdateRequest request, @MappingTarget ClassSchedule entity);
}
