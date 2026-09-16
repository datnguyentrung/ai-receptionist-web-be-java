package com.dat.ai_receptionist_web.mapper.Core;

import com.dat.ai_receptionist_web.domain.Core.Branch;
import com.dat.ai_receptionist_web.dto.Core.BranchDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BranchMapper {
    @Mapping(target = "branchId", source = "branchId")
    BranchDTO.Response toResponse(Branch entity);

    @Mapping(target = "branchId", source = "branchId")
    BranchDTO.SimpleResponse toSimpleResponse(Branch entity);

    @Mapping(target = "branchId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Branch toEntity(BranchDTO.CreateRequest request);

    List<BranchDTO.Response> toResponseList(List<Branch> branches);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "hotline", source = "hotline")
    @Mapping(target = "openedDate", source = "openedDate")
    @Mapping(target = "status", source = "status")
    void updateEntity(BranchDTO.UpdateRequest request, @MappingTarget Branch entity);
}
