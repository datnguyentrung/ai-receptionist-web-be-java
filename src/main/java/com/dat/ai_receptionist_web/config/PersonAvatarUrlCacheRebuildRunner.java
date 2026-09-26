package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.service.Core.PersonAvatarUrlCacheRebuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "avatar.cache.rebuild.enabled", havingValue = "true")
@Slf4j
public class PersonAvatarUrlCacheRebuildRunner implements ApplicationRunner {
    private final PersonAvatarUrlCacheRebuildService rebuildService;
    private final Environment environment;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        requireNonWebMode();
        PersonAvatarUrlCacheRebuildService.RebuildResult result = rebuildService.rebuild();
        log.info("PERSON_AVATAR_CACHE_REBUILD_COMMAND_COMPLETED generation={} scanned={} cached={}",
                result.generation(), result.scanned(), result.cached());
        applicationContext.close();
    }

    private void requireNonWebMode() {
        String webType = environment.getProperty("spring.main.web-application-type", "servlet");
        if (!"none".equalsIgnoreCase(webType)) {
            throw new IllegalStateException("Avatar cache rebuild must run with spring.main.web-application-type=none");
        }
    }
}
