package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.service.Core.PersonFaceImageUrlResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = PositionMapper.class)
public abstract class PersonMapper {
    @Autowired
    private PositionMapper positionMapper;

    @Autowired
    private PersonFaceImageUrlResolver faceImageUrlResolver;

    public PersonDTO.Response toResponse(Person entity) {
        if (entity == null) {
            return null;
        }
        return new PersonDTO.Response(
                entity.getPersonId(),
                entity.getFullName(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getEmail(),
                entity.getNationalCode(),
                entity.getPersonCode(),
                entity.getCurrentBelt(),
                entity.getStatus(),
                entity.getStartDate(),
                positionMapper.toSimpleResponse(entity.getPosition()),
                signedFaceImageUrl(entity),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PersonDTO.SimpleResponse toSimpleResponse(Person entity) {
        if (entity == null) {
            return null;
        }
        return new PersonDTO.SimpleResponse(
                entity.getPersonId(),
                entity.getFullName(),
                entity.getGender(),
                entity.getBirthDate(),
                entity.getPersonCode(),
                entity.getCurrentBelt(),
                entity.getStatus(),
                signedFaceImageUrl(entity)
        );
    }

    public PersonDTO.BriefResponse toBriefResponse(Person entity) {
        if (entity == null) {
            return null;
        }
        return new PersonDTO.BriefResponse(
                entity.getPersonId(),
                entity.getFullName(),
                entity.getPersonCode(),
                entity.getCurrentBelt(),
                entity.getStatus(),
                signedFaceImageUrl(entity)
        );
    }

    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "personCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "faceEmbedding", ignore = true)
    public abstract Person toEntity(PersonDTO.CreateRequest request);

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
    public abstract void updateEntity(PersonDTO.UpdateRequest request, @MappingTarget Person entity);

    private String signedFaceImageUrl(Person entity) {
        return faceImageUrlResolver.resolve(entity.getPersonId(), entity.getFaceImagePath());
    }
}
