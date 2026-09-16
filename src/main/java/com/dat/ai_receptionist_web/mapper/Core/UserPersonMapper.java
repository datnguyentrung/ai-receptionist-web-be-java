package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.dto.Core.UserPersonDTO;
import com.dat.ai_receptionist_web.dto.Security.LoginRes;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {UserMapper.class, PersonMapper.class})
public interface UserPersonMapper {
    UserPersonDTO.Response toResponse(UserPerson entity);

    UserPersonDTO.SimpleResponse toSimpleResponse(UserPerson entity);

    default LoginRes.UserContextRes toLoginContext(UserPerson entity) {
        if (entity == null) {
            return null;
        }
        return new LoginRes.UserContextRes(
                entity.getUserPersonId(),
                entity.getPerson() == null ? null : entity.getPerson().getPersonId(),
                entity.getRelationshipType(),
                entity.getPerson() == null ? null : entity.getPerson().getPersonCode(),
                entity.getPerson() == null ? null : entity.getPerson().getFullName()
        );
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "relationshipType", source = "relationshipType")
    @Mapping(target = "active", source = "active")
    void updateEntity(UserPersonDTO.UpdateRequest request, @MappingTarget UserPerson entity);
}
