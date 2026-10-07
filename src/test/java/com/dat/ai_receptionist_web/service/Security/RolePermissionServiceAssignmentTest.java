package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import com.dat.ai_receptionist_web.enums.Security.PermissionDefinition;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RolePermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RolePermissionServiceAssignmentTest {
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final RolePermissionService service =
            new RolePermissionService(roleRepository, permissionRepository, rolePermissionRepository, mock(com.dat.ai_receptionist_web.mapper.Security.RolePermissionMapper.class));

    @Test
    void firstSyncAddsMissingPermissionsAndIncrementsVersionOnce() {
        Role role = role("SUPER_ADMIN", 4);
        when(roleRepository.findById("SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.findByIdForUpdate("SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findPermissionCodes("SUPER_ADMIN"))
                .thenReturn(new TreeSet<>(Set.of(PermissionDefinition.BRANCH_READ.getCode())))
                .thenReturn(new TreeSet<>(Set.of(PermissionDefinition.BRANCH_READ.getCode())));
        when(permissionRepository.findAllByCodeIn(Set.of(
                PermissionDefinition.BRANCH_CREATE.getCode(),
                PermissionDefinition.BRANCH_UPDATE.getCode()
        ))).thenReturn(List.of(
                permission(11, PermissionDefinition.BRANCH_CREATE),
                permission(12, PermissionDefinition.BRANCH_UPDATE)
        ));

        RolePermissionService.SyncResult result = service.replaceInternal("super_admin", Set.of(
                PermissionDefinition.BRANCH_READ.getCode(),
                PermissionDefinition.BRANCH_CREATE.getCode(),
                PermissionDefinition.BRANCH_UPDATE.getCode()
        ));

        assertThat(result.addedCount()).isEqualTo(2);
        assertThat(result.removedCount()).isZero();
        assertThat(result.permissionVersion()).isEqualTo(5);
        verify(roleRepository, times(1)).incrementPermissionVersion("SUPER_ADMIN");
        verify(rolePermissionRepository, never()).deleteByRoleCodeAndPermissionCodeIn(anyString(), anySet());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RolePermission>> saved = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(2);
    }

    @Test
    void unchangedSyncDoesNotWriteOrIncrementVersion() {
        Role role = role("SUPER_ADMIN", 7);
        Set<String> desired = Set.of(PermissionDefinition.BRANCH_READ.getCode());
        when(roleRepository.findById("SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findPermissionCodes("SUPER_ADMIN")).thenReturn(new TreeSet<>(desired));

        RolePermissionService.SyncResult result = service.replaceInternal("SUPER_ADMIN", desired);

        assertThat(result.addedCount()).isZero();
        assertThat(result.removedCount()).isZero();
        assertThat(result.permissionVersion()).isEqualTo(7);
        verify(roleRepository, never()).findByIdForUpdate(anyString());
        verify(roleRepository, never()).incrementPermissionVersion(anyString());
        verify(permissionRepository, never()).findAllByCodeIn(anySet());
        verify(rolePermissionRepository, never()).saveAll(any());
        verify(rolePermissionRepository, never()).deleteByRoleCodeAndPermissionCodeIn(anyString(), anySet());
    }

    @Test
    void mixedAddRemoveIncrementsVersionOnce() {
        Role role = role("SYSTEM_ADMIN", 3);
        when(roleRepository.findById("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.findByIdForUpdate("SYSTEM_ADMIN")).thenReturn(Optional.of(role));
        when(rolePermissionRepository.findPermissionCodes("SYSTEM_ADMIN")).thenReturn(new TreeSet<>(Set.of(
                PermissionDefinition.BRANCH_READ.getCode(),
                PermissionDefinition.WALLET_READ.getCode()
        ))).thenReturn(new TreeSet<>(Set.of(
                PermissionDefinition.BRANCH_READ.getCode(),
                PermissionDefinition.WALLET_READ.getCode()
        )));
        when(permissionRepository.findAllByCodeIn(Set.of(PermissionDefinition.PERSON_READ.getCode())))
                .thenReturn(List.of(permission(21, PermissionDefinition.PERSON_READ)));

        RolePermissionService.SyncResult result = service.replaceInternal("SYSTEM_ADMIN", Set.of(
                PermissionDefinition.BRANCH_READ.getCode(),
                PermissionDefinition.PERSON_READ.getCode()
        ));

        assertThat(result.addedCount()).isEqualTo(1);
        assertThat(result.removedCount()).isEqualTo(1);
        assertThat(result.permissionVersion()).isEqualTo(4);
        verify(roleRepository, times(1)).incrementPermissionVersion("SYSTEM_ADMIN");
        verify(rolePermissionRepository).deleteByRoleCodeAndPermissionCodeIn(
                "SYSTEM_ADMIN",
                Set.of(PermissionDefinition.WALLET_READ.getCode())
        );
    }

    private Role role(String code, long permissionVersion) {
        return new Role(code, "Super Administrator", "Has all permissions", permissionVersion);
    }

    private Permission permission(Integer id, PermissionDefinition definition) {
        return Permission.builder()
                .permissionId(id)
                .code(definition.getCode())
                .model(definition.getModel())
                .action(definition.getAction())
                .description(definition.getDescription())
                .build();
    }
}
