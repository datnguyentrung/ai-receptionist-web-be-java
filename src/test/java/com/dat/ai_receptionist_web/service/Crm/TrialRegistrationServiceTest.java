package com.dat.ai_receptionist_web.service.Crm;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Crm.Lead;
import com.dat.ai_receptionist_web.domain.Crm.TrialRegistration;
import com.dat.ai_receptionist_web.domain.Training.ClassSession;
import com.dat.ai_receptionist_web.dto.Crm.TrialRegistrationDTO;
import com.dat.ai_receptionist_web.enums.Crm.TrialRegistrationStatus;
import com.dat.ai_receptionist_web.mapper.Crm.TrialRegistrationMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Crm.LeadRepository;
import com.dat.ai_receptionist_web.repository.Crm.TrialRegistrationRepository;
import com.dat.ai_receptionist_web.repository.Training.ClassSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrialRegistrationServiceTest {
    private final TrialRegistrationRepository repository =
            mock(TrialRegistrationRepository.class);
    private final LeadRepository leads = mock(LeadRepository.class);
    private final ClassSessionRepository classSessions = mock(ClassSessionRepository.class);
    private final PersonRepository people = mock(PersonRepository.class);
    private final TrialRegistrationMapper mapper = mock(TrialRegistrationMapper.class);
    private final TrialRegistrationService service = new TrialRegistrationService(
            repository, leads, classSessions, people, mapper);

    @Test
    void createsTrialRegistrationWithLeadClassSessionAndCoach() {
        UUID leadId = UUID.randomUUID();
        UUID classSessionId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        Lead lead = Lead.builder().leadId(leadId).build();
        ClassSession classSession = ClassSession.builder().classSessionId(classSessionId).build();
        Person coach = Person.builder().personId(coachId).build();
        TrialRegistrationDTO.Response response = mock(TrialRegistrationDTO.Response.class);
        TrialRegistrationDTO.CreateRequest request = new TrialRegistrationDTO.CreateRequest(
                leadId, classSessionId, coachId, TrialRegistrationStatus.BOOKED,
                null, "On time", "Follow up");

        when(leads.findById(leadId)).thenReturn(Optional.of(lead));
        when(classSessions.findById(classSessionId)).thenReturn(Optional.of(classSession));
        when(people.findById(coachId)).thenReturn(Optional.of(coach));
        when(repository.save(any(TrialRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(TrialRegistration.class))).thenReturn(response);

        assertThat(service.create(request)).isSameAs(response);

        ArgumentCaptor<TrialRegistration> captor = ArgumentCaptor.forClass(TrialRegistration.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLead()).isSameAs(lead);
        assertThat(captor.getValue().getClassSession()).isSameAs(classSession);
        assertThat(captor.getValue().getCoachPerson()).isSameAs(coach);
    }
}
