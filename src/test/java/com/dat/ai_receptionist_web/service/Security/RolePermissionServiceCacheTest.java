package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Permission;
import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.domain.Security.RolePermission;
import com.dat.ai_receptionist_web.dto.Security.RolePermissionDTO;
import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import com.dat.ai_receptionist_web.mapper.Security.RolePermissionMapper;
import com.dat.ai_receptionist_web.repository.Security.PermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RolePermissionRepository;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RolePermissionServiceCacheTest {

    @Test
    void createEvictsRoleListCache() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(CachingTestConfiguration.class)) {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            Cache cache = cacheManager.getCache("roleList");
            assertThat(cache).isNotNull();
            cache.put("page:0:size:20:sort:code,ASC,NATIVE,false", "cached");

            RoleRepository roleRepository = context.getBean(RoleRepository.class);
            PermissionRepository permissionRepository = context.getBean(PermissionRepository.class);
            RolePermissionRepository rolePermissionRepository = context.getBean(RolePermissionRepository.class);
            RolePermissionMapper mapper = context.getBean(RolePermissionMapper.class);
            RolePermissionService service = context.getBean(RolePermissionService.class);

            Role role = new Role("GUARDIAN", "Guardian", "Guardian role", 1);
            Permission permission = Permission.builder()
                    .permissionId(1)
                    .code("STUDENT_ENROLLMENT_READ")
                    .model("STUDENT_ENROLLMENT")
                    .action(PermissionAction.READ)
                    .description("Student Enrollment Read")
                    .build();
            RolePermission rolePermission = new RolePermission(
                    new RolePermission.Key(role.getCode(), permission.getPermissionId()),
                    role,
                    permission
            );

            when(roleRepository.findById("GUARDIAN")).thenReturn(Optional.of(role));
            when(permissionRepository.findById(1)).thenReturn(Optional.of(permission));
            when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(rolePermission);
            when(mapper.toResponse(any(RolePermission.class)))
                    .thenReturn(new RolePermissionDTO.ItemResponse("GUARDIAN", 1, "STUDENT_ENROLLMENT_READ"));

            service.create(new RolePermissionDTO.CreateRequest("GUARDIAN", 1));

            assertThat(cache.get("page:0:size:20:sort:code,ASC,NATIVE,false")).isNull();
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
        PermissionRepository permissionRepository() {
            return mock(PermissionRepository.class);
        }

        @Bean
        RolePermissionRepository rolePermissionRepository() {
            return mock(RolePermissionRepository.class);
        }

        @Bean
        RolePermissionMapper rolePermissionMapper() {
            return mock(RolePermissionMapper.class);
        }

        @Bean
        RolePermissionService rolePermissionService(
                RoleRepository roleRepository,
                PermissionRepository permissionRepository,
                RolePermissionRepository rolePermissionRepository,
                RolePermissionMapper rolePermissionMapper
        ) {
            return new RolePermissionService(
                    roleRepository,
                    permissionRepository,
                    rolePermissionRepository,
                    rolePermissionMapper
            );
        }
    }
}
