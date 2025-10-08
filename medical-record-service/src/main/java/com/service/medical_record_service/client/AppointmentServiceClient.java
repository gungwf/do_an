package com.service.medical_record_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "appointment-service")
public interface AppointmentServiceClient {
    @GetMapping("/appointments/{id}")
    AppointmentResponseDto getAppointmentById(@PathVariable("id") UUID id);
}