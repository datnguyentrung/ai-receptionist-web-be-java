package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserRole;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import com.dat.ai_receptionist_web.enums.Security.UserStatus;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.mapper.Security.RoleMapper;
import com.dat.ai_receptionist_web.mapper.Security.UserMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRoleRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
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
    @Mock
    UserRoleRepository userRoleRepository;
    @Mock
    RoleMapper roleMapper;
    @InjectMocks
    UserService service;

    @Test
    void getIncludesBriefPersonsAndBriefRoles() {
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        User user = user(userId, "0900000001");
        Person person = Person.builder()
                .personId(personId)
                .fullName("Nguyen Van A")
                .personCode("HV001")
                .build();
        Role role = Role.builder()
                .code("MANAGER")
                .name("Manager")
                .permissionVersion(3L)
                .build();
        UserPerson userPerson = UserPerson.builder()
                .user(user)
                .person(person)
                .active(true)
                .build();
        UserRole userRole = new UserRole(new UserRole.Key(userId, "MANAGER"), user, role);
        PersonDTO.BriefResponse personResponse = new PersonDTO.BriefResponse(
                personId,
                "Nguyen Van A",
                "HV001",
                null,
                null,
                null
        );
        RoleDTO.BriefResponse roleResponse = new RoleDTO.BriefResponse("MANAGER", "Manager", 3L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userPersonRepository.findAllByUser_UserIdAndActiveTrue(userId)).thenReturn(List.of(userPerson));
        when(personMapper.toBriefResponse(person)).thenReturn(personResponse);
        when(userRoleRepository.findAllDetailedByUserId(userId)).thenReturn(List.of(userRole));
        when(roleMapper.toBriefResponse(role)).thenReturn(roleResponse);

        var response = service.get(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.phoneNumber()).isEqualTo("0900000001");
        assertThat(response.persons()).containsExactly(personResponse);
        assertThat(response.roles()).containsExactly(roleResponse);
        verify(userPersonRepository).findAllByUser_UserIdAndActiveTrue(userId);
        verify(userRoleRepository).findAllDetailedByUserId(userId);
    }

    @Test
    void listFetchesBriefPersonsForCurrentUserPageInOneBatch() {
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
        PersonDTO.BriefResponse personResponse = new PersonDTO.BriefResponse(
                personId,
                "Nguyen Van A",
                "HV001",
                null,
                null,
                null
        );

        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user, otherUser), pageable, 2));
        when(userPersonRepository.findAllActiveByUserIds(Set.of(userId, otherUserId))).thenReturn(List.of(userPerson));
        when(personMapper.toBriefResponse(person)).thenReturn(personResponse);

        var response = service.list(pageable);

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).persons()).containsExactly(personResponse);
        assertThat(response.getContent().get(1).persons()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(2);
        verify(userPersonRepository).findAllActiveByUserIds(Set.of(userId, otherUserId));
    }

    private User user(UUID userId, String phoneNumber) {
        return User.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
