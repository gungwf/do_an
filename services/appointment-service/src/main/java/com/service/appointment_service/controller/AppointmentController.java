package com.service.appointment_service.controller;


import com.service.appointment_service.client.dto.UserDto;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.dto.request.AppointmentRequest;
import com.service.appointment_service.dto.response.AppointmentResponseDto;
import com.service.appointment_service.dto.request.UpdateAppointmentStatusRequest;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final UserServiceClient userServiceClient;

    //tạo appointment
    @PostMapping
    @PreAuthorize("hasAnyAuthority('patient', 'staff', 'doctor')")
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
    public ResponseEntity<List<AppointmentResponseDto>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    // update appointment
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> updateAppointment(
            @PathVariable UUID id,
            @RequestBody AppointmentRequest request
    ) {
        AppointmentResponseDto updatedAppointment = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(updatedAppointment);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('patient')") // Chỉ bệnh nhân mới được tự hủy lịch
    public ResponseEntity<?> cancelAppointment(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        // Lấy email của bệnh nhân từ token
        String patientEmail = authentication.getName();
        // Gọi sang sys-srv để lấy ID của bệnh nhân
        UserDto patient = userServiceClient.getUserByEmail(patientEmail);

        // Gọi service để thực hiện hủy lịch
        AppointmentResponseDto canceledAppointment = appointmentService.cancelAppointment(id, patient.id());
        return ResponseEntity.ok(canceledAppointment);
    }
}