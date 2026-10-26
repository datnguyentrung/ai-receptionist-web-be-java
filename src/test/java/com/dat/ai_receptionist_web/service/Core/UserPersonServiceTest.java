package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.mapper.Core.UserPersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPersonServiceTest {
    @Test
    void listsAllLinksWhenPersonIdIsNotProvided() {
        UserPersonRepository repository = mock(UserPersonRepository.class);
        Pageable pageable = Pageable.ofSize(20);
        when(repository.findAllDetailed(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        UserPersonService service = service(repository);

        assertThat(service.list(pageable, null).getContent()).isEmpty();
        verify(repository).findAllDetailed(pageable);
    }

    @Test
    void listsOnlyTheRequestedPersonsLinksAndKeepsEmptyPagesEmpty() {
        UserPersonRepository repository = mock(UserPersonRepository.class);
        Pageable pageable = Pageable.ofSize(20);
        UUID personId = UUID.randomUUID();
        when(repository.findAllDetailedByPersonId(personId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        UserPersonService service = service(repository);

        assertThat(service.list(pageable, personId).getContent()).isEmpty();
        verify(repository).findAllDetailedByPersonId(personId, pageable);
    }

    private static UserPersonService service(UserPersonRepository repository) {
        return new UserPersonService(
                repository,
                mock(UserPersonMapper.class),
                mock(UserRepository.class),
                mock(PersonRepository.class)
        );
    }
}
