package com.dat.ai_receptionist_web.mapper.Training;

import com.dat.ai_receptionist_web.domain.Training.BeltExam;
import com.dat.ai_receptionist_web.dto.Training.BeltExamDTO;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PersonMapper.class, UserMapper.class})
public interface BeltExamMapper {
    BeltExamDTO.Response toResponse(BeltExam entity);

    BeltExamDTO.SimpleResponse toSimpleResponse(BeltExam entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "createdByUser", ignore = true)
    @Mapping(target = "fromBelt", source = "fromBelt")
    @Mapping(target = "targetBelt", source = "targetBelt")
    @Mapping(target = "year", source = "year")
    @Mapping(target = "quarter", source = "quarter")
    @Mapping(target = "examDate", source = "examDate")
    @Mapping(target = "result", source = "result")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "type", source = "type")
    void updateEntity(BeltExamDTO.UpdateRequest request, @MappingTarget BeltExam entity);
}
