package com.service.medical_record_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableMethodSecurity
public class MedicalRecordServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicalRecordServiceApplication.class, args);
	}

}
