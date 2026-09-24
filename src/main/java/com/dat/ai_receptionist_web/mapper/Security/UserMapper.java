package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Security.UserDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "persons", ignore = true)
    @Mapping(target = "roles", ignore = true)
    UserDTO.Response toResponse(User entity);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "persons", ignore = true)
    UserDTO.SimpleResponse toSimpleResponse(User entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "authorizationVersion", source = "authorizationVersion")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    void updateEntity(UserDTO.UpdateRequest request, @MappingTarget User entity);
}
