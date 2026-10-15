package com.dat.ai_receptionist_web.config;

import com.dat.ai_receptionist_web.socket.ClassSessionWebSocketHandler;
import com.dat.ai_receptionist_web.socket.SparringWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClassSessionWebSocketHandler classSessionWebSocketHandler;
    private final SparringWebSocketHandler sparringWebSocketHandler;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(classSessionWebSocketHandler, "/ws/class-sessions")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
        registry.addHandler(sparringWebSocketHandler, "/ws/sparring")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }
}
