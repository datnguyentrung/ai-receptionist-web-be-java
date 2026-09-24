package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = PositionMapper.class)
public interface PersonMapper {
    @Mapping(target = "personId", source = "personId")
    PersonDTO.Response toResponse(Person entity);

    @Mapping(target = "personId", source = "personId")
    PersonDTO.SimpleResponse toSimpleResponse(Person entity);

    @Mapping(target = "personId", source = "personId")
    PersonDTO.BriefResponse toBriefResponse(Person entity);

    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "personCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "faceEmbedding", ignore = true)
    Person toEntity(PersonDTO.CreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "nationalCode", source = "nationalCode")
    @Mapping(target = "faceImagePath", source = "faceImagePath")
    @Mapping(target = "personCode", source = "personCode")
    @Mapping(target = "currentBelt", source = "currentBelt")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "position", ignore = true)
    void updateEntity(PersonDTO.UpdateRequest request, @MappingTarget Person entity);
}
