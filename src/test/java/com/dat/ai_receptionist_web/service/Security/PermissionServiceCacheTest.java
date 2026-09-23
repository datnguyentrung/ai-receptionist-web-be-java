package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.dto.Security.PermissionDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import com.dat.ai_receptionist_web.mapper.Security.PermissionMapper;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PermissionServiceCacheTest {

    @Test
    void listCachesByPageable() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            PermissionRepository repository = context.getBean(PermissionRepository.class);
            PermissionMapper mapper = context.getBean(PermissionMapper.class);
            PermissionService service = context.getBean(PermissionService.class);

            Permission permission = permission(1, "PERMISSION_READ");
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(permission)));
            when(mapper.toSimpleResponse(permission)).thenReturn(simpleResponse(permission));

            PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());

            assertThat(service.list(pageable).getContent()).hasSize(1);
            assertThat(service.list(pageable).getContent()).hasSize(1);

            verify(repository, times(1)).findAll(pageable);
        }
    }

    @Test
    void listUsesDifferentCacheEntriesForDifferentPageables() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            PermissionRepository repository = context.getBean(PermissionRepository.class);
            PermissionMapper mapper = context.getBean(PermissionMapper.class);
            PermissionService service = context.getBean(PermissionService.class);

            Permission permission = permission(1, "PERMISSION_READ");
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(permission)));
            when(mapper.toSimpleResponse(permission)).thenReturn(simpleResponse(permission));

            PageRequest firstPage = PageRequest.of(0, 20, Sort.by("code").ascending());
            PageRequest secondPage = PageRequest.of(1, 20, Sort.by("code").ascending());

            service.list(firstPage);
            service.list(secondPage);

            verify(repository, times(1)).findAll(firstPage);
            verify(repository, times(1)).findAll(secondPage);
        }
    }

    @Test
    void createEvictsPermissionListCache() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            PermissionRepository repository = context.getBean(PermissionRepository.class);
            PermissionMapper mapper = context.getBean(PermissionMapper.class);
            PermissionService service = context.getBean(PermissionService.class);

            Permission permission = permission(1, "PERMISSION_READ");
            when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(permission)));
            when(repository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(mapper.toSimpleResponse(permission)).thenReturn(simpleResponse(permission));
            when(mapper.toResponse(any(Permission.class))).thenAnswer(invocation -> {
                Permission saved = invocation.getArgument(0);
                return new PermissionDTO.Response(
                        saved.getPermissionId(),
                        saved.getCode(),
                        saved.getModel(),
                        saved.getAction(),
                        saved.getDescription()
                );
            });

            PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());

            service.list(pageable);
            service.list(pageable);
            verify(repository, times(1)).findAll(pageable);

            service.create(new PermissionDTO.CreateRequest(
                    "PERMISSION_CREATE",
                    "permission",
                    PermissionAction.CREATE,
                    "Create permissions"
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
            return new ConcurrentMapCacheManager("permissionList");
        }

        @Bean
        PermissionRepository permissionRepository() {
            return mock(PermissionRepository.class);
        }

        @Bean
        PermissionMapper permissionMapper() {
            return mock(PermissionMapper.class);
        }

        @Bean
        PermissionService permissionService(PermissionRepository repository, PermissionMapper mapper) {
            return new PermissionService(repository, mapper);
        }
    }

    private static Permission permission(Integer id, String code) {
        return Permission.builder()
                .permissionId(id)
                .code(code)
                .model("permission")
                .action(PermissionAction.READ)
                .description("Read permissions")
                .build();
    }

    private static PermissionDTO.SimpleResponse simpleResponse(Permission permission) {
        return new PermissionDTO.SimpleResponse(
                permission.getPermissionId(),
                permission.getCode(),
                permission.getModel(),
                permission.getAction()
        );
    }
}
