package com.dat.ai_receptionist_web.mapper.Skill;

import com.dat.ai_receptionist_web.domain.Skill.FitnessRecord;
import com.dat.ai_receptionist_web.dto.Skill.FitnessRecordDTO;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, FitnessMapper.class})
public interface FitnessRecordMapper {
    FitnessRecordDTO.Response toResponse(FitnessRecord entity);

    FitnessRecordDTO.SimpleResponse toSimpleResponse(FitnessRecord entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "fitness", ignore = true)
    @Mapping(target = "recordedByCoach", ignore = true)
    @Mapping(target = "recordDate", source = "recordDate")
    @Mapping(target = "duration", source = "duration")
    void updateEntity(FitnessRecordDTO.UpdateRequest request, @MappingTarget FitnessRecord entity);
}
