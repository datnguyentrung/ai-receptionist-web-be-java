package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    @Mapping(target = "permissionId", source = "permissionId")
    PermissionDTO.Response toResponse(Permission entity);

    @Mapping(target = "permissionId", source = "permissionId")
    PermissionDTO.SimpleResponse toSimpleResponse(Permission entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "description", source = "description")
    void updateEntity(PermissionDTO.UpdateRequest request, @MappingTarget Permission entity);
}
