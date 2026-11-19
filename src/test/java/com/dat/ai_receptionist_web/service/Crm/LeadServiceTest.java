package com.dat.ai_receptionist_web.service.Crm;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Crm.Lead;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Crm.LeadDTO;
import com.dat.ai_receptionist_web.enums.Crm.LeadStatus;
import com.dat.ai_receptionist_web.error.ApiException;
import com.dat.ai_receptionist_web.error.code.CrmErrorCode;
import com.dat.ai_receptionist_web.mapper.Crm.LeadMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Crm.LeadRepository;
import com.dat.ai_receptionist_web.repository.Crm.TrialRegistrationRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeadServiceTest {
    private final LeadRepository repository = mock(LeadRepository.class);
    private final TrialRegistrationRepository trialRegistrations =
            mock(TrialRegistrationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PersonRepository people = mock(PersonRepository.class);
    private final LeadMapper mapper = mock(LeadMapper.class);
    private final LeadService service = new LeadService(
            repository, trialRegistrations, users, people, mapper);

    @Test
    void createsLeadAndResolvesOptionalRelations() {
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        Person person = Person.builder().personId(personId).build();
        LeadDTO.Response response = mock(LeadDTO.Response.class);
        LeadDTO.CreateRequest request = new LeadDTO.CreateRequest(
                "Nguyen Van A", "0901234567", "a@example.com", LocalDate.of(2000, 1, 1),
                LeadStatus.NEW, userId, LocalDateTime.now().plusDays(1), null,
                "Can goi lai", personId, null);

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(people.findById(personId)).thenReturn(Optional.of(person));
        when(repository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Lead.class))).thenReturn(response);

        assertThat(service.create(request)).isSameAs(response);

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAssignedUser()).isSameAs(user);
        assertThat(captor.getValue().getConvertedPerson()).isSameAs(person);
    }

    @Test
    void blocksDeletingLeadThatHasTrialRegistrations() {
        UUID leadId = UUID.randomUUID();
        Lead lead = Lead.builder().leadId(leadId).build();
        when(repository.findById(leadId)).thenReturn(Optional.of(lead));
        when(trialRegistrations.existsByLead_LeadId(leadId)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(leadId))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).getErrorCode())
                        .isEqualTo(CrmErrorCode.LEAD_IN_USE));
        verify(repository, never()).delete(any(Lead.class));
    }
}
