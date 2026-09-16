package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "code", source = "code")
    Role toEntity(RoleDTO.CreateRequest request);

    @Mapping(target = "code", ignore = true)
    Role toEntity(RoleDTO.UpdateRequest request);

    @Mapping(target = "code", source = "code")
    RoleDTO.Response toResponse(Role entity);

    @Mapping(target = "code", source = "code")
    RoleDTO.SimpleResponse toSimpleResponse(Role entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "permissionVersion", source = "permissionVersion")
    void updateEntity(RoleDTO.UpdateRequest request, @MappingTarget Role entity);
}
