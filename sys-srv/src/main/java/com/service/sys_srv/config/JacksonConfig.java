package com.service.sys_srv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // Thêm import này
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Thêm import này
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Thêm module để xử lý Hibernate proxies (đã có)
        objectMapper.registerModule(new Hibernate5JakartaModule());

        // 2. xử lý các kiểu ngày tháng Java 8 (LocalDate, OffsetDateTime...)
        objectMapper.registerModule(new JavaTimeModule());

//ngày tháng được ghi ra dưới dạng chuỗi (vd: "1995-08-15") thay vì dạng số (timestamp).
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}