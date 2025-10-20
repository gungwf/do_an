package com.reporting_service.reporting_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "appointment-service")
public interface AppointmentServiceClient {
    @GetMapping("/appointments")
    List<AppointmentResponseDto> getAllAppointments();
}