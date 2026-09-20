package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.service.Core.PersonService;
import com.dat.ai_receptionist_web.service.Core.UserPersonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    PersonRepository personRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    UserPersonService userPersonService;
    @Mock
    PersonService personService;
    @Mock
    UserPersonRepository userPersonRepository;
    @Mock
    PersonMapper personMapper;
    @InjectMocks
    UserService service;

    @Test
    void listFetchesPersonsForCurrentUserPageInOneBatch() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        User user = user(userId, "0900000001");
        User otherUser = user(otherUserId, "0900000002");
        Person person = Person.builder()
                .personId(personId)
                .fullName("Nguyen Van A")
                .personCode("HV001")
                .build();
        UserPerson userPerson = UserPerson.builder()
                .user(user)
                .person(person)
                .active(true)
                .build();
        PersonDTO.SimpleResponse personResponse = new PersonDTO.SimpleResponse(
                personId,
                "Nguyen Van A",
                null,
                null,
                "HV001",
                null,
                null,
                null
        );

        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user, otherUser), pageable, 2));
        when(userPersonRepository.findAllActiveByUserIds(Set.of(userId, otherUserId))).thenReturn(List.of(userPerson));
        when(personMapper.toSimpleResponse(person)).thenReturn(personResponse);

        var response = service.list(pageable);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).userId()).isEqualTo(userId);
        assertThat(response.getContent().get(0).persons()).containsExactly(personResponse);
        assertThat(response.getContent().get(1).userId()).isEqualTo(otherUserId);
        assertThat(response.getContent().get(1).persons()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(2);
        verify(userPersonRepository).findAllActiveByUserIds(Set.of(userId, otherUserId));
    }

    @Test
    void listDoesNotFetchPersonsWhenUserPageIsEmpty() {
        var pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = service.list(pageable);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.isEmpty()).isTrue();
        verifyNoInteractions(userPersonRepository);
    }

    @Test
    void listUsesSearchQueryForNonBlankSearchAndStillFetchesPersonsInOneBatch() {
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        User user = user(userId, "0901000001");
        Person person = Person.builder()
                .personId(personId)
                .fullName("Nguyen Van A")
                .personCode("HV001")
                .build();
        UserPerson userPerson = UserPerson.builder()
                .user(user)
                .person(person)
                .active(true)
                .build();
        PersonDTO.SimpleResponse personResponse = new PersonDTO.SimpleResponse(
                personId,
                "Nguyen Van A",
                null,
                null,
                "HV001",
                null,
                null,
                null
        );

        when(userRepository.searchByPhoneNumberOrPersonFullName("nguyen", "", pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));
        when(userPersonRepository.findAllActiveByUserIds(Set.of(userId))).thenReturn(List.of(userPerson));
        when(personMapper.toSimpleResponse(person)).thenReturn(personResponse);

        var response = service.list("  Nguyen  ", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().persons()).containsExactly(personResponse);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(userRepository).searchByPhoneNumberOrPersonFullName("nguyen", "", pageable);
        verify(userPersonRepository).findAllActiveByUserIds(Set.of(userId));
    }

    @Test
    void listNormalizesPartialVietnamPhoneSearchWithoutRequiringACompletePhoneNumber() {
        var pageable = PageRequest.of(0, 20);
        when(userRepository.searchByPhoneNumberOrPersonFullName("+84 901", "0901", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var response = service.list("+84 901", pageable);

        assertThat(response.getContent()).isEmpty();
        verify(userRepository).searchByPhoneNumberOrPersonFullName("+84 901", "0901", pageable);
        verifyNoInteractions(userPersonRepository);
    }

    private User user(UUID userId, String phoneNumber) {
        return User.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
