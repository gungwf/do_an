package com.service.medical_record_service.client.client;

import com.service.medical_record_service.client.dto.AppointmentResponseDto;
import com.service.medical_record_service.client.dto.InternalStatusUpdateRequest;
import com.service.medical_record_service.client.dto.PagedAppointmentResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "appointment-service")
public interface AppointmentServiceClient {
    @GetMapping("/appointments/{id}")
    AppointmentResponseDto getAppointmentById(@PathVariable("id") UUID id);

    @GetMapping("/appointments/patient/{patientId}")
    PagedAppointmentResponse getAppointmentsForPatient(
        @PathVariable("patientId") java.util.UUID patientId,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "20") int size
    );

    @PatchMapping("/{id}/set-service")
    ResponseEntity<Void> setServiceForAppointment(@PathVariable UUID id,@RequestBody Map<String, UUID> payload);

    @PutMapping("/appointments/{id}/internal-status")
    void updateAppointmentStatusInternal(
        @PathVariable("id") UUID appointmentId,
        @RequestBody InternalStatusUpdateRequest request
    );
}