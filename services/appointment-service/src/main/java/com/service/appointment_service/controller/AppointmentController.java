package com.service.appointment_service.controller;

import com.service.appointment_service.client.dto.UserDto;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.dto.request.AppointmentRequest;
import com.service.appointment_service.dto.request.AppointmentSearchRequest;
import com.service.appointment_service.dto.request.DoctorAppointmentSearchRequest;
import com.service.appointment_service.dto.request.StaffAppointmentSearchRequest;
import com.service.appointment_service.client.dto.StaffSearchRequest;
import com.service.appointment_service.client.dto.StaffSearchResponseDto;
import com.service.appointment_service.dto.request.InternalStatusUpdateRequest;
import com.service.appointment_service.dto.response.AppointmentResponseDto;
import com.service.appointment_service.dto.request.UpdateAppointmentStatusRequest;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    private final UserServiceClient userServiceClient;

    // tạo appointment
    @PostMapping
    public ResponseEntity<?> createAppointment(
            Authentication authentication,
            @RequestBody AppointmentRequest request) {
        try {
            String patientEmail = authentication.getName();
            Appointment newAppointment = appointmentService.createAppointment(patientEmail, request);
            return ResponseEntity.ok(appointmentService.mapToResponseDto(newAppointment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // get appointment by id
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(appointmentService.getAppointmentById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // get appointment cho bệnh nhân (with pagination & sort by updatedAt desc)
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<org.springframework.data.domain.Page<AppointmentResponseDto>> getAppointmentsForPatient(
            @PathVariable UUID patientId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(appointmentService.getAppointmentsForPatient(patientId, page, size));
    }

    // get appointment cho bác sĩ
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentsForDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsForDoctor(doctorId));
    }

    // update status appointment
    @PatchMapping("/status/{id}")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable UUID id,
            @RequestBody UpdateAppointmentStatusRequest request) {
        try {
            AppointmentResponseDto updatedAppointment = appointmentService.updateAppointmentStatus(id,
                    request.getStatus());
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
            @RequestBody AppointmentRequest request) {
        AppointmentResponseDto updatedAppointment = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(updatedAppointment);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable UUID id,
            Authentication authentication) {
        // Lấy email của bệnh nhân từ token
        String patientEmail = authentication.getName();
        // Gọi sang sys-srv để lấy ID của bệnh nhân
        UserDto patient = userServiceClient.getUserByEmail(patientEmail);

        // Gọi service để thực hiện hủy lịch
        AppointmentResponseDto canceledAppointment = appointmentService.cancelAppointment(id, patient.id());
        return ResponseEntity.ok(canceledAppointment);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<AppointmentResponseDto>> searchAppointments(
            @RequestBody AppointmentSearchRequest request) {
        return ResponseEntity.ok(appointmentService.searchAppointments(request));
    }

    @PutMapping("/{id}/internal-status")
    public ResponseEntity<Void> updateAppointmentStatusInternal(
            @PathVariable UUID id,
            @RequestBody InternalStatusUpdateRequest request) {
        appointmentService.updateAppointmentStatusFromInternal(id, request.status());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/doctor/{doctorId}/appointments")
    public Page<AppointmentResponseDto> search(
            @PathVariable UUID doctorId,
            @RequestBody DoctorAppointmentSearchRequest req) {
        return appointmentService.searchAppointmentsForDoctor(doctorId, req);
    }

    @PostMapping("/staff/appointments")
    public Page<AppointmentResponseDto> searchForStaff(
            Authentication authentication,
            @RequestBody StaffAppointmentSearchRequest req) {
        String staffEmail = authentication.getName();
        StaffSearchRequest staffReq = new StaffSearchRequest();
        staffReq.setEmail(staffEmail);
        staffReq.setRole("staff");
        staffReq.setSize(1);
        Page<StaffSearchResponseDto> staffPage = userServiceClient.searchStaffs(staffReq);
        if (staffPage.isEmpty()) {
            throw new RuntimeException("Staff not found or no branch assigned");
        }
        UUID branchId = staffPage.getContent().get(0).getBranchId();
        if (branchId == null) {
            throw new RuntimeException("Staff has no branch assigned");
        }
        return appointmentService.searchAppointmentsForStaff(branchId, req);
    }

}