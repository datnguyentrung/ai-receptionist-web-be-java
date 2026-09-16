package com.dat.ai_receptionist_web.mapper.Skill;

import com.dat.ai_receptionist_web.domain.Skill.Fitness;
import com.dat.ai_receptionist_web.dto.Skill.FitnessDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FitnessMapper {
    @Mapping(target = "fitnessId", source = "fitnessId")
    FitnessDTO.Response toResponse(Fitness entity);

    @Mapping(target = "fitnessId", source = "fitnessId")
    FitnessDTO.SimpleResponse toSimpleResponse(Fitness entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "scheduleLevel", source = "scheduleLevel")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "duration", source = "duration")
    void updateEntity(FitnessDTO.UpdateRequest request, @MappingTarget Fitness entity);
}
