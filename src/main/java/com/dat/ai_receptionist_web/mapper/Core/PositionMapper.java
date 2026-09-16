package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Position;
import com.dat.ai_receptionist_web.dto.Core.PositionDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PositionMapper {
    PositionDTO.Response toResponse(Position entity);

    PositionDTO.SimpleResponse toSimpleResponse(Position entity);

    @Mapping(target = "positionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Position toEntity(PositionDTO.CreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "active", source = "active")
    void updateEntity(PositionDTO.UpdateRequest request, @MappingTarget Position entity);
}
