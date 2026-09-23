package com.dat.ai_receptionist_web.config;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.scheduling.annotation.EnableAsync;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
@EnableAsync
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.connect-timeout:10s}")
    private Duration redisConnectTimeout;

    @Value("${spring.data.redis.timeout:10s}")
    private Duration redisCommandTimeout;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    @Value("${spring.data.redis.ssl.disable-peer-verification:true}")
    private boolean redisDisablePeerVerification;

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

        // Chỉ set user/pass nếu trong .env có (phòng lỗi null)
        if (redisUsername != null && !redisUsername.isEmpty()) {
            config.setUsername(redisUsername);
        }
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(redisCommandTimeout)
                        .shutdownTimeout(Duration.ofMillis(100))
                        .clientOptions(ClientOptions.builder()
                                .socketOptions(SocketOptions.builder()
                                        .connectTimeout(redisConnectTimeout)
                                        .build())
                                .build());

        if (redisSslEnabled) {
            LettuceClientConfiguration.LettuceSslClientConfigurationBuilder sslBuilder = clientBuilder.useSsl();
            if (redisDisablePeerVerification) {
                sslBuilder.disablePeerVerification();
            }
            sslBuilder.and();
        }
        log.info(
                "Redis connection configured host={} port={} usernameConfigured={} sslEnabled={} connectTimeout={} commandTimeout={}",
                redisHost,
                redisPort,
                redisUsername != null && !redisUsername.isBlank(),
                redisSslEnabled,
                redisConnectTimeout,
                redisCommandTimeout
        );
        return new LettuceConnectionFactory(config, clientBuilder.build());
    }

    @Bean
    public RedisSerializer<Object> redisValueSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .customize(mapper -> mapper.activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                        DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.WRAPPER_ARRAY))
                // Cache values are deserialized as Object; force the writer to use the
                // same root type so immutable lists also retain their collection type id.
                .writer((mapper, value) -> mapper.writerFor(Object.class).writeValueAsBytes(value))
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisSerializer<Object> redisValueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(lettuceConnectionFactory());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisValueSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(redisValueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(lettuceConnectionFactory());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration(RedisSerializer<Object> redisValueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(7)) // TTL mặc định 7 ngày
                .disableCachingNullValues()
                // Thêm prefix "app_name:" hoặc để default "::" tùy bạn, ở đây mình config dùng dấu ":" cho đẹp
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("⚠️ Redis GET error (Key: {}): {}", key, exception.getMessage());
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("⚠️ Redis PUT error (Key: {}): {}", key, exception.getMessage());
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("⚠️ Redis EVICT error (Key: {}): {}", key, exception.getMessage());
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("⚠️ Redis CLEAR error: {}", exception.getMessage());
            }
        };
    }

    @Bean(name = "redisCacheManager")
    @Primary
    public RedisCacheManager redisCacheManager(
            LettuceConnectionFactory connectionFactory,
            CacheTtlConfig ttlConfig,
            RedisCacheConfiguration cacheConfiguration
    ) {

        // 1. Lấy cấu hình mặc định (dùng JSON serializer và TTL 7 ngày mà bạn đã setup)
        RedisCacheConfiguration defaultConfig = cacheConfiguration;

        // 2. Map riêng từng TTL cho từng loại Tên Cache (Tên khai báo trong //@Cacheable)
        Map<String, RedisCacheConfiguration> specificCacheConfigs = new HashMap<>();

        // ===== NHÓM 1: DỮ LIỆU ĐỘNG (Thay đổi liên tục) -> TTL: 1 NGÀY =====
        // Các detail thường chứa Sĩ số, Phân công... nên cho chết sớm để update
        specificCacheConfigs.put("coachDetail", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("coachDetailByCode", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("classScheduleDetail", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("studentEnrollmentsById", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("studentEnrollmentsByCode", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("studentEnrollmentsByClass", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("singleEnrollment", defaultConfig.entryTtl(ttlConfig.randomOneDay()));
        specificCacheConfigs.put("fcmTokensByRole", defaultConfig.entryTtl(ttlConfig.randomOneHour()));

        // ===== NHÓM 2: DỮ LIỆU ÍT ĐỔI (Thông tin cơ bản) -> TTL: 1 TUẦN =====
        specificCacheConfigs.put("coach", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));
        specificCacheConfigs.put("coachByCode", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));
        specificCacheConfigs.put("classSchedule", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));
        specificCacheConfigs.put("permissionList", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));
        specificCacheConfigs.put("roleList", defaultConfig.entryTtl(ttlConfig.randomOneWeek()));

        // 3. Build CacheManager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig) // Nếu quên cấu hình tên nào, nó sẽ dùng mặc định 7 ngày
                .withInitialCacheConfigurations(specificCacheConfigs) // Nạp cấu hình riêng vào
                .build();
    }

    @Bean
    public ApplicationRunner cacheManagerStartupLogger(Map<String, CacheManager> cacheManagers) {
        return args -> {
            cacheManagers.forEach((name, cacheManager) ->
                    log.info("CacheManager bean [{}] = {}", name, cacheManager.getClass().getName())
            );
            CacheManager redisCacheManager = cacheManagers.get("redisCacheManager");
            log.info("Spring cache annotations explicitly use CacheManager [redisCacheManager] = {}",
                    redisCacheManager == null ? "<missing>" : redisCacheManager.getClass().getName());
        };
    }
}
