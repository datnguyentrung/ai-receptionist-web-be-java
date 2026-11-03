package com.dat.ai_receptionist_web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);

        // Các phương thức HTTP được phép
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // CÁC HEADER ĐƯỢC PHÉP GỬI LÊN (Chỉ gọi 1 lần và có chứa ngrok-skip-browser-warning)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "x-no-retry", "ngrok-skip-browser-warning"));

        // Cho phép gửi cookie, JWT,...
        configuration.setAllowCredentials(true);
        // Cache pre-flight request trong 1 giờ
        configuration.setMaxAge(3600L);
        // Áp dụng cho tất cả endpoint HTTP, bao gồm các request REST cấp socket-ticket.
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
