package com.dat.ai_receptionist_web.config.Supabase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class SupabaseConfig {
    @Bean("supabaseStorageRestClient")
    @ConditionalOnExpression("'${supabase.url:}'.length() > 0 && '${supabase.service-role-key:}'.length() > 0")
    public RestClient supabaseStorageRestClient(SupabaseProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(stripTrailingSlash(properties.getUrl()) + "/storage/v1")
                .requestFactory(requestFactory)
                .defaultHeader("apikey", properties.getServiceRoleKey())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getServiceRoleKey())
                .build();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
