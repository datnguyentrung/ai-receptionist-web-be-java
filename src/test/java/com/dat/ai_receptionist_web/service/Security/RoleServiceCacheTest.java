package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import com.dat.ai_receptionist_web.dto.Security.RoleDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import com.dat.ai_receptionist_web.mapper.Security.RoleMapper;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class RoleServiceCacheTest {

    @Test
    void listCachesByPageable() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            RoleRepository repository = context.getBean(RoleRepository.class);
            RoleMapper mapper = context.getBean(RoleMapper.class);
            RoleService service = context.getBean(RoleService.class);

            Role role = role("SYSTEM_ADMIN");
            List<RoleRepository.RolePermissionRow> permissionRows = List.of(permissionRow("SYSTEM_ADMIN"));
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(role)));
            when(repository.findPermissionsByRoleCodeIn(Set.of("SYSTEM_ADMIN"))).thenReturn(permissionRows);
            when(mapper.groupPermissionRowsByRoleCode(permissionRows)).thenReturn(Map.of("SYSTEM_ADMIN", permissionRows));
            when(mapper.toSimpleResponse(role, permissionRows)).thenReturn(simpleResponse(role, List.of(simplePermission())));

            PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());

            assertThat(service.list(pageable).getContent()).hasSize(1);
            assertThat(service.list(pageable).getContent()).hasSize(1);

            verify(repository, times(1)).findAll(pageable);
            verify(repository, times(1)).findPermissionsByRoleCodeIn(Set.of("SYSTEM_ADMIN"));
        }
    }

    @Test
    void listUsesDifferentCacheEntriesForDifferentPageables() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            RoleRepository repository = context.getBean(RoleRepository.class);
            RoleMapper mapper = context.getBean(RoleMapper.class);
            RoleService service = context.getBean(RoleService.class);

            Role role = role("SYSTEM_ADMIN");
            List<RoleRepository.RolePermissionRow> permissionRows = List.of(permissionRow("SYSTEM_ADMIN"));
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(role)));
            when(repository.findPermissionsByRoleCodeIn(Set.of("SYSTEM_ADMIN"))).thenReturn(permissionRows);
            when(mapper.groupPermissionRowsByRoleCode(permissionRows)).thenReturn(Map.of("SYSTEM_ADMIN", permissionRows));
            when(mapper.toSimpleResponse(role, permissionRows)).thenReturn(simpleResponse(role, List.of(simplePermission())));

            PageRequest firstPage = PageRequest.of(0, 20, Sort.by("code").ascending());
            PageRequest secondPage = PageRequest.of(1, 20, Sort.by("code").ascending());

            service.list(firstPage);
            service.list(secondPage);

            verify(repository, times(1)).findAll(firstPage);
            verify(repository, times(1)).findAll(secondPage);
        }
    }

    @Test
    void createEvictsRoleListCache() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            RoleRepository repository = context.getBean(RoleRepository.class);
            RoleMapper mapper = context.getBean(RoleMapper.class);
            RoleService service = context.getBean(RoleService.class);

            Role role = role("SYSTEM_ADMIN");
            List<RoleRepository.RolePermissionRow> permissionRows = List.of(permissionRow("SYSTEM_ADMIN"));
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(role)));
            when(repository.findPermissionsByRoleCodeIn(Set.of("SYSTEM_ADMIN"))).thenReturn(permissionRows);
            when(mapper.groupPermissionRowsByRoleCode(permissionRows)).thenReturn(Map.of("SYSTEM_ADMIN", permissionRows));
            when(repository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(mapper.toSimpleResponse(role, permissionRows)).thenReturn(simpleResponse(role, List.of(simplePermission())));
            when(mapper.toResponse(any(Role.class), anyList())).thenAnswer(invocation -> {
                Role saved = invocation.getArgument(0);
                List<RoleRepository.RolePermissionRow> savedPermissions = invocation.getArgument(1);
                return new RoleDTO.Response(
                        saved.getCode(),
                        saved.getName(),
                        saved.getDescription(),
                        saved.getPermissionVersion(),
                        savedPermissions.stream()
                                .map(row -> new PermissionDTO.Response(
                                        row.getPermissionId(),
                                        row.getPermissionCode(),
                                        row.getPermissionModel(),
                                        row.getPermissionAction(),
                                        row.getPermissionDescription()
                                ))
                                .toList()
                );
            });

            PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());

            service.list(pageable);
            service.list(pageable);
            verify(repository, times(1)).findAll(pageable);

            service.create(new RoleDTO.CreateRequest(
                    "STAFF",
                    "Staff",
                    "Staff role",
                    1
            ));
            service.list(pageable);

            verify(repository, times(2)).findAll(pageable);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CachingTestConfiguration {
        @Bean(name = "redisCacheManager")
        CacheManager redisCacheManager() {
            return new ConcurrentMapCacheManager("roleList");
        }

        @Bean
        RoleRepository roleRepository() {
            return mock(RoleRepository.class);
        }

        @Bean
        RoleMapper roleMapper() {
            return mock(RoleMapper.class);
        }

        @Bean
        RolePermissionService rolePermissionService() {
            return mock(RolePermissionService.class);
        }

        @Bean
        RoleService roleService(
                RoleRepository repository,
                RoleMapper mapper,
                RolePermissionService rolePermissionService
        ) {
            return new RoleService(repository, mapper, rolePermissionService);
        }
    }

    private static Role role(String code) {
        return Role.builder()
                .code(code)
                .name("System Administrator")
                .description("System administrator role")
                .permissionVersion(1)
                .build();
    }

    private static RoleDTO.SimpleResponse simpleResponse(Role role) {
        return simpleResponse(role, List.of());
    }

    private static RoleDTO.SimpleResponse simpleResponse(
            Role role,
            List<PermissionDTO.SimpleResponse> permissions
    ) {
        return new RoleDTO.SimpleResponse(
                role.getCode(),
                role.getName(),
                role.getPermissionVersion(),
                permissions
        );
    }

    private static PermissionDTO.SimpleResponse simplePermission() {
        return new PermissionDTO.SimpleResponse(1, "ROLE_READ", "ROLE", PermissionAction.READ);
    }

    private static RoleRepository.RolePermissionRow permissionRow(String roleCode) {
        return new RolePermissionRowStub(roleCode);
    }

    private record RolePermissionRowStub(String roleCode) implements RoleRepository.RolePermissionRow {
        @Override
        public String getRoleCode() {
            return roleCode;
        }

        @Override
        public Integer getPermissionId() {
            return 1;
        }

        @Override
        public String getPermissionCode() {
            return "ROLE_READ";
        }

        @Override
        public String getPermissionModel() {
            return "ROLE";
        }

        @Override
        public PermissionAction getPermissionAction() {
            return PermissionAction.READ;
        }

        @Override
        public String getPermissionDescription() {
            return "Role Read";
        }
    }
}
