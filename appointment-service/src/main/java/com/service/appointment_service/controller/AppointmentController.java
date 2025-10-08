package com.service.appointment_service.controller;


import com.service.appointment_service.dto.AppointmentRequest;
import com.service.appointment_service.dto.AppointmentResponseDto;
import com.service.appointment_service.dto.UpdateAppointmentStatusRequest;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AppointmentController {
    private final AppointmentService appointmentService;

    //tạo appointment
    @PostMapping
    @PreAuthorize("hasAnyAuthority('patient', 'staff')")
    public ResponseEntity<?> createAppointment(
            Authentication authentication,
            @RequestBody AppointmentRequest request
    ) {
        try {
            String patientEmail = authentication.getName();
            Appointment newAppointment = appointmentService.createAppointment(patientEmail, request);
            return ResponseEntity.ok(appointmentService.mapToResponseDto(newAppointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //get appointment by id
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(appointmentService.getAppointmentById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // get appointment cho bệnh nhân
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('patient')")
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentsForPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsForPatient(patientId));
    }

    // get appointment cho bác sĩ
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasAnyAuthority('doctor', 'staff')")
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentsForDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsForDoctor(doctorId));
    }

    // update status appointment
    @PatchMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('doctor', 'staff')")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable UUID id,
            @RequestBody UpdateAppointmentStatusRequest request
    ) {
        try {
            AppointmentResponseDto updatedAppointment = appointmentService.updateAppointmentStatus(id, request.getStatus());
            return ResponseEntity.ok(updatedAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // get all appointment
    @GetMapping
    @PreAuthorize("hasAnyAuthority('admin', 'staff')")
    public ResponseEntity<List<AppointmentResponseDto>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    // update appointment
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'staff')")
    public ResponseEntity<AppointmentResponseDto> updateAppointment(
            @PathVariable UUID id,
            @RequestBody AppointmentRequest request
    ) {
        AppointmentResponseDto updatedAppointment = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(updatedAppointment);
    }
}