package com.service.sys_srv.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/ws-chat/**")
            .allowedOrigins("http://localhost:63342", "http://localhost:4200") // dev origins
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true); // nếu client cần gửi cookies / auth headers
      }
    };
  }
}
