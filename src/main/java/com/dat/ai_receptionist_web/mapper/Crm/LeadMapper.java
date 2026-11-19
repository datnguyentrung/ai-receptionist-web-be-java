package com.dat.ai_receptionist_web.mapper.Crm;

import com.dat.ai_receptionist_web.domain.Crm.Lead;
import com.dat.ai_receptionist_web.dto.Crm.LeadDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LeadMapper {
    @Mapping(target = "assignedUserId", source = "assignedUser.userId")
    @Mapping(target = "convertedPersonId", source = "convertedPerson.personId")
    LeadDTO.Response toResponse(Lead entity);

    @Mapping(target = "assignedUserId", source = "assignedUser.userId")
    LeadDTO.SimpleResponse toSimpleResponse(Lead entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "nextFollowUpAt", source = "nextFollowUpAt")
    @Mapping(target = "lastContactedAt", source = "lastContactedAt")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "convertedAt", source = "convertedAt")
    void updateEntity(LeadDTO.CreateRequest request, @MappingTarget Lead entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "nextFollowUpAt", source = "nextFollowUpAt")
    @Mapping(target = "lastContactedAt", source = "lastContactedAt")
    @Mapping(target = "note", source = "note")
    @Mapping(target = "convertedAt", source = "convertedAt")
    void updateEntity(LeadDTO.UpdateRequest request, @MappingTarget Lead entity);
}
