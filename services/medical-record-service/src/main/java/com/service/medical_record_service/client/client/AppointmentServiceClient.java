package com.service.medical_record_service.client.client;

import com.service.medical_record_service.client.dto.AppointmentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "appointment-service")
public interface AppointmentServiceClient {
    @GetMapping("/appointments/{id}")
    AppointmentResponseDto getAppointmentById(@PathVariable("id") UUID id);

    @PatchMapping("/{id}/set-service")
    ResponseEntity<Void> setServiceForAppointment(@PathVariable UUID id,@RequestBody Map<String, UUID> payload);
}