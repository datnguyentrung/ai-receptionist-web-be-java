package com.dat.ai_receptionist_web.service.Crm;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Crm.Lead;
import com.dat.ai_receptionist_web.domain.Crm.TrialRegistration;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Crm.TrialRegistrationDTO;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CoreErrorCode;
import com.dat.ai_receptionist_web.error.code.CrmErrorCode;
import com.dat.ai_receptionist_web.error.code.TrainingErrorCode;
import com.dat.ai_receptionist_web.mapper.Crm.TrialRegistrationMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Crm.LeadRepository;
import com.dat.ai_receptionist_web.repository.Crm.TrialRegistrationRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrialRegistrationService {
    private final TrialRegistrationRepository repository;
    private final LeadRepository leadRepository;
    private final ClassSessionRepository classSessionRepository;
    private final PersonRepository personRepository;
    private final TrialRegistrationMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<TrialRegistrationDTO.SimpleResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable), mapper::toSimpleResponse);
    }

    @Transactional(readOnly = true)
    public TrialRegistrationDTO.Response get(UUID id) {
        return mapper.toResponse(findDetailed(id));
    }

    @Transactional
    public TrialRegistrationDTO.Response create(TrialRegistrationDTO.CreateRequest request) {
        TrialRegistration entity = new TrialRegistration();
        mapper.updateEntity(request, entity);
        entity.setLead(lead(request.leadId()));
        applyRelations(entity, request.classSessionId(), request.coachPersonId());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public TrialRegistrationDTO.Response update(UUID id, TrialRegistrationDTO.UpdateRequest request) {
        TrialRegistration entity = findDetailed(id);
        mapper.updateEntity(request, entity);
        applyRelations(entity, request.classSessionId(), request.coachPersonId());
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findDetailed(id));
    }

    private TrialRegistration findDetailed(UUID id) {
        return repository.findDetailedByTrialRegistrationId(id)
                .orElseThrow(() -> new ApiException(CrmErrorCode.TRIAL_REGISTRATION_NOT_FOUND));
    }

    private Lead lead(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ApiException(CrmErrorCode.LEAD_NOT_FOUND));
    }

    private void applyRelations(TrialRegistration entity, UUID classSessionId, UUID coachPersonId) {
        entity.setClassSession(classSession(classSessionId));
        entity.setCoachPerson(person(coachPersonId));
    }

    private ClassSession classSession(UUID id) {
        if (id == null) {
            return null;
        }
        return classSessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(TrainingErrorCode.CLASS_SESSION_NOT_FOUND));
    }

    private Person person(UUID id) {
        if (id == null) {
            return null;
        }
        return personRepository.findById(id)
                .orElseThrow(() -> new ApiException(CoreErrorCode.PERSON_NOT_FOUND));
    }
}
