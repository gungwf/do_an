package com.service.sys_srv.chat.config;

import com.service.sys_srv.chat.socket.JwtHandshakeInterceptor;
import com.service.sys_srv.chat.socket.StompPrincipalHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

  public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor) {
    this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-chat")
        .addInterceptors(jwtHandshakeInterceptor)
        .setHandshakeHandler(new StompPrincipalHandshakeHandler())
        .addInterceptors(jwtHandshakeInterceptor)
        .setAllowedOriginPatterns("*") // or list origins explicitly
        .withSockJS();
  }
}
