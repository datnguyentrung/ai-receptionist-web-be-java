package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.config.BootstrapUserProperties;
import com.dat.ai_receptionist_web.domain.Core.Person;
import com.dat.ai_receptionist_web.domain.Core.UserPerson;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.enums.Security.RelationshipType;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.repository.Core.UserPersonRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SystemUserSynchronizerTest {
    private final BootstrapUserProperties properties = new BootstrapUserProperties();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PersonRepository personRepository = mock(PersonRepository.class);
    private final UserPersonRepository userPersonRepository = mock(UserPersonRepository.class);
    private final UserService userService = mock(UserService.class);
    private final UserRoleService userRoleService = mock(UserRoleService.class);
    private final SystemUserSynchronizer synchronizer = new SystemUserSynchronizer(
            properties,
            userRepository,
            personRepository,
            userPersonRepository,
            userService,
            userRoleService
    );

    @Test
    void createsMissingUserPersonContextAndRoleAssignment() {
        BootstrapUserProperties.UserAccount account = account();
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        User user = User.builder().userId(userId).phoneNumber("0900000001").build();

        when(userRepository.findAllByPhoneNumberIn(Set.of("0900000001"))).thenReturn(List.of());
        when(personRepository.findAllByPersonCodeUpperIn(Set.of("VQ_SUPER_ADMIN"))).thenReturn(List.of());
        when(userService.createLoginUserWithoutDuplicateCheck("0900000001", "secret")).thenReturn(user);
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person person = invocation.getArgument(0);
            person.setPersonId(personId);
            return person;
        });
        when(userPersonRepository.findAllByUserIdInAndPersonIdInAndRelationshipType(
                Set.of(userId), Set.of(personId), RelationshipType.MANAGER
        )).thenReturn(List.of());
        when(userRoleService.assignRolesIfMissing(Map.of(userId, Set.of("SUPER_ADMIN")))).thenReturn(1);

        SystemUserSynchronizer.SyncResult result = synchronizer.syncAll(List.of(account));

        assertThat(result.usersCreated()).isEqualTo(1);
        assertThat(result.personsCreated()).isEqualTo(1);
        assertThat(result.contextsCreated()).isEqualTo(1);
        assertThat(result.roleAssignments()).isEqualTo(1);
        verify(userService).createLoginUserWithoutDuplicateCheck("0900000001", "secret");
        verify(personRepository).save(any(Person.class));
        verify(userPersonRepository).save(any(UserPerson.class));
        verify(userRoleService).assignRolesIfMissing(Map.of(userId, Set.of("SUPER_ADMIN")));
    }

    @Test
    void unchangedRestartUsesBulkReadsAndDoesNotCreateOrLock() {
        BootstrapUserProperties.UserAccount account = account();
        UUID userId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        User user = User.builder().userId(userId).phoneNumber("0900000001").build();
        Person person = Person.builder().personId(personId).personCode("VQ_SUPER_ADMIN").build();
        UserPerson userPerson = UserPerson.builder()
                .user(user)
                .person(person)
                .relationshipType(RelationshipType.MANAGER)
                .active(true)
                .build();

        when(userRepository.findAllByPhoneNumberIn(Set.of("0900000001"))).thenReturn(List.of(user));
        when(personRepository.findAllByPersonCodeUpperIn(Set.of("VQ_SUPER_ADMIN"))).thenReturn(List.of(person));
        when(userPersonRepository.findAllByUserIdInAndPersonIdInAndRelationshipType(
                Set.of(userId), Set.of(personId), RelationshipType.MANAGER
        )).thenReturn(List.of(userPerson));
        when(userRoleService.assignRolesIfMissing(Map.of(userId, Set.of("SUPER_ADMIN")))).thenReturn(0);

        SystemUserSynchronizer.SyncResult result = synchronizer.syncAll(List.of(account));

        assertThat(result.usersCreated()).isZero();
        assertThat(result.personsCreated()).isZero();
        assertThat(result.contextsCreated()).isZero();
        assertThat(result.contextsActivated()).isZero();
        assertThat(result.roleAssignments()).isZero();
        verify(userService, never()).createLoginUserWithoutDuplicateCheck(anyString(), anyString());
        verify(personRepository, never()).save(any());
        verify(userPersonRepository, never()).save(any());
        verify(userRoleService).assignRolesIfMissing(Map.of(userId, Set.of("SUPER_ADMIN")));
    }

    @Test
    void rejectsLegacyVqtBootstrapCode() {
        BootstrapUserProperties.UserAccount account = account();
        account.setPersonCode("VQT_SUPER_ADMIN");

        assertThatThrownBy(() -> synchronizer.syncAll(List.of(account)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bootstrap user personCode must start with VQ_");
    }

    private BootstrapUserProperties.UserAccount account() {
        BootstrapUserProperties.UserAccount account = new BootstrapUserProperties.UserAccount();
        account.setRoleCode("SUPER_ADMIN");
        account.setPhoneNumber("0900000001");
        account.setPassword("secret");
        account.setPersonCode("VQ_SUPER_ADMIN");
        account.setFullName("System Super Administrator");
        account.setEmail("super.admin@example.local");
        return account;
    }
}
