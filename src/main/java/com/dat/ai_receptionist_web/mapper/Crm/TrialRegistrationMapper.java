package com.dat.ai_receptionist_web.mapper.Crm;

import com.dat.ai_receptionist_web.domain.Crm.TrialRegistration;
import com.dat.ai_receptionist_web.dto.Crm.TrialRegistrationDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TrialRegistrationMapper {
    @Mapping(target = "leadId", source = "lead.leadId")
    @Mapping(target = "leadFullName", source = "lead.fullName")
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    @Mapping(target = "coachPersonId", source = "coachPerson.personId")
    @Mapping(target = "coachPersonName", source = "coachPerson.fullName")
    TrialRegistrationDTO.Response toResponse(TrialRegistration entity);

    @Mapping(target = "leadId", source = "lead.leadId")
    @Mapping(target = "leadFullName", source = "lead.fullName")
    @Mapping(target = "classSessionId", source = "classSession.classSessionId")
    TrialRegistrationDTO.SimpleResponse toSimpleResponse(TrialRegistration entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "checkedInAt", source = "checkedInAt")
    @Mapping(target = "coachFeedback", source = "coachFeedback")
    @Mapping(target = "outcomeNote", source = "outcomeNote")
    void updateEntity(TrialRegistrationDTO.CreateRequest request, @MappingTarget TrialRegistration entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "status", source = "status")
    @Mapping(target = "checkedInAt", source = "checkedInAt")
    @Mapping(target = "coachFeedback", source = "coachFeedback")
    @Mapping(target = "outcomeNote", source = "outcomeNote")
    void updateEntity(TrialRegistrationDTO.UpdateRequest request, @MappingTarget TrialRegistration entity);
}
