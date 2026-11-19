package com.dat.ai_receptionist_web.service.Crm;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Crm.Lead;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Crm.LeadDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.CrmErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import com.dat.ai_receptionist_web.mapper.Crm.LeadMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Crm.LeadRepository;
import com.dat.ai_receptionist_web.repository.Crm.TrialRegistrationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadService {
    private final LeadRepository repository;
    private final TrialRegistrationRepository trialRegistrationRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final LeadMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<LeadDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public LeadDTO.Response get(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public LeadDTO.Response create(LeadDTO.CreateRequest request) {
        Lead entity = new Lead();
        mapper.updateEntity(request, entity);
        applyRelations(entity, request.assignedUserId(), request.convertedPersonId());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public LeadDTO.Response update(UUID id, LeadDTO.UpdateRequest request) {
        Lead entity = find(id);
        mapper.updateEntity(request, entity);
        applyRelations(entity, request.assignedUserId(), request.convertedPersonId());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        Lead entity = find(id);
        if (trialRegistrationRepository.existsByLead_LeadId(id)) {
            throw new ApiException(CrmErrorCode.LEAD_IN_USE);
        }
        repository.delete(entity);
    }

    private Lead find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(CrmErrorCode.LEAD_NOT_FOUND));
    }

    private void applyRelations(Lead entity, UUID assignedUserId, UUID convertedPersonId) {
        entity.setAssignedUser(user(assignedUserId));
        entity.setConvertedPerson(person(convertedPersonId));
    }

    private User user(UUID id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(SecurityErrorCode.USER_NOT_FOUND));
    }

    private Person person(UUID id) {
        if (id == null) {
            return null;
        }
        return personRepository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
    }
}
