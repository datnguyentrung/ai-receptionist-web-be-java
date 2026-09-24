package com.dat.ai_receptionist_web.mapper.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "code", source = "code")
    Role toEntity(RoleDTO.CreateRequest request);

    @Mapping(target = "code", ignore = true)
    Role toEntity(RoleDTO.UpdateRequest request);

    default RoleDTO.Response toResponse(
            Role entity,
            List<RoleRepository.RolePermissionRow> permissionRows
    ) {
        return new RoleDTO.Response(
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getPermissionVersion(),
                toPermissionResponses(permissionRows)
        );
    }

    default RoleDTO.SimpleResponse toSimpleResponse(
            Role entity,
            List<RoleRepository.RolePermissionRow> permissionRows
    ) {
        return new RoleDTO.SimpleResponse(
                entity.getCode(),
                entity.getName(),
                entity.getPermissionVersion(),
                toSimplePermissionResponses(permissionRows)
        );
    }

    RoleDTO.BriefResponse toBriefResponse(Role entity);

    default Map<String, List<RoleRepository.RolePermissionRow>> groupPermissionRowsByRoleCode(
            List<RoleRepository.RolePermissionRow> permissionRows
    ) {
        if (permissionRows == null || permissionRows.isEmpty()) {
            return Map.of();
        }

        return permissionRows.stream()
                .collect(Collectors.groupingBy(
                        RoleRepository.RolePermissionRow::getRoleCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    default List<PermissionDTO.Response> toPermissionResponses(
            List<RoleRepository.RolePermissionRow> permissionRows
    ) {
        if (permissionRows == null || permissionRows.isEmpty()) {
            return List.of();
        }

        return permissionRows.stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    default List<PermissionDTO.SimpleResponse> toSimplePermissionResponses(
            List<RoleRepository.RolePermissionRow> permissionRows
    ) {
        if (permissionRows == null || permissionRows.isEmpty()) {
            return List.of();
        }

        return permissionRows.stream()
                .map(this::toSimplePermissionResponse)
                .toList();
    }

    @Mapping(target = "permissionId", source = "permissionId")
    @Mapping(target = "code", source = "permissionCode")
    @Mapping(target = "model", source = "permissionModel")
    @Mapping(target = "action", source = "permissionAction")
    @Mapping(target = "description", source = "permissionDescription")
    PermissionDTO.Response toPermissionResponse(RoleRepository.RolePermissionRow row);

    @Mapping(target = "permissionId", source = "permissionId")
    @Mapping(target = "code", source = "permissionCode")
    @Mapping(target = "model", source = "permissionModel")
    @Mapping(target = "action", source = "permissionAction")
    PermissionDTO.SimpleResponse toSimplePermissionResponse(RoleRepository.RolePermissionRow row);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "permissionVersion", source = "permissionVersion")
    void updateEntity(RoleDTO.UpdateRequest request, @MappingTarget Role entity);
}
