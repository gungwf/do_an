//package com.service.api_gateway.config;
//
//import org.springdoc.core.properties.SwaggerUiConfigProperties;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Comparator;
//
//@Configuration
//public class SwaggerConfig {
//
//    @Bean
//    public CommandLineRunner openApiGroups(
//            DiscoveryClient discoveryClient,
//            SwaggerUiConfigProperties swaggerUiConfigProperties) {
//        return args -> {
//            // Lấy danh sách các service đã đăng ký với Eureka
//            discoveryClient.getServices().stream()
//                    .sorted()
//                    // Lọc ra các service nghiệp vụ
//                    .filter(serviceId -> serviceId.endsWith("-service"))
//                    .forEach(serviceId -> {
//                        // Xây dựng URL để truy cập file api-docs của service con
//                        String url = String.format("/%s/v3/api-docs", serviceId);
//
//                        // Sử dụng phương thức getUrls().add() để thêm URL mới
//                        swaggerUiConfigProperties.getUrls().add(
//                                new SwaggerUiConfigProperties.SwaggerUrl(serviceId, url, null)
//                        );
//                    });
//        };
//    }
//}