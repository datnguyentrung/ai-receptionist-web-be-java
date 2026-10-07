package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.User;
import com.dat.ai_receptionist_web.domain.Security.UserRole;
import com.dat.ai_receptionist_web.mapper.Security.UserRoleMapper;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRepository;
import com.dat.ai_receptionist_web.repository.Security.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserRoleServiceTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final UserRoleService service = new UserRoleService(
            userRepository,
            roleRepository,
            userRoleRepository,
            mock(UserRoleMapper.class)
    );

    @Test
    void assignRolesIfMissingDoesNotLockOrWriteWhenAssignmentsExist() {
        UUID userId = UUID.randomUUID();
        UserRoleRepository.UserRoleRow row = mock(UserRoleRepository.UserRoleRow.class);
        when(row.getUserId()).thenReturn(userId);
        when(row.getRoleCode()).thenReturn("SUPER_ADMIN");
        when(userRoleRepository.findAllByUserIdIn(Set.of(userId))).thenReturn(List.of(row));

        int created = service.assignRolesIfMissing(Map.of(userId, Set.of("SUPER_ADMIN")));

        assertThat(created).isZero();
        verify(userRepository, never()).findAllByUserIdInForUpdate(anySet());
        verify(roleRepository, never()).findAllById(any());
        verify(userRoleRepository, never()).saveAll(any());
        verify(userRepository, never()).incrementAuthorizationVersion(any());
    }

    @Test
    void assignRolesIfMissingLocksAfterDiffRechecksAndIncrementsOnce() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().userId(userId).phoneNumber("0900000001").build();
        Role role = new Role("SUPER_ADMIN", "Super Administrator", "Has all permissions", 0);
        when(userRoleRepository.findAllByUserIdIn(Set.of(userId)))
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(userRepository.findAllByUserIdInForUpdate(Set.of(userId))).thenReturn(List.of(user));
        when(roleRepository.findAllById(Set.of("SUPER_ADMIN"))).thenReturn(List.of(role));

        int created = service.assignRolesIfMissing(Map.of(userId, Set.of("super_admin")));

        assertThat(created).isEqualTo(1);
        verify(userRepository).findAllByUserIdInForUpdate(Set.of(userId));
        verify(userRepository).incrementAuthorizationVersion(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserRole>> saved = ArgumentCaptor.forClass(List.class);
        verify(userRoleRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        assertThat(saved.getValue().getFirst().getId().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getFirst().getId().getRoleId()).isEqualTo("SUPER_ADMIN");
    }
}
