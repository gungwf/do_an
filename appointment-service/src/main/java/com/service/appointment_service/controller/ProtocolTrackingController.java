package com.service.appointment_service.controller;

import com.service.appointment_service.client.dto.UserDto;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.dto.response.ProtocolTrackingResponseDto;
import com.service.appointment_service.dto.request.StartProtocolRequest;
import com.service.appointment_service.entity.ProtocolTracking;
import com.service.appointment_service.service.ProtocolTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/protocol-tracking")
@RequiredArgsConstructor
public class ProtocolTrackingController {

    private final ProtocolTrackingService protocolTrackingService;
    private final UserServiceClient userServiceClient;

    @PostMapping("/start")
    // Cho phép cả patient, staff, và admin gọi API này
    @PreAuthorize("hasAnyAuthority('patient', 'staff', 'admin')")
    public ResponseEntity<?> startProtocol(
            @RequestBody StartProtocolRequest request,
            Authentication authentication
    ) {
        UUID patientIdToUse;

        // Lấy danh sách quyền của người dùng từ token
        boolean isStaffOrAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority ->
                        grantedAuthority.getAuthority().equals("staff") ||
                                grantedAuthority.getAuthority().equals("admin"));

        if (isStaffOrAdmin) {
            // TRƯỜNG HỢP 1: Nhân viên/Admin thực hiện
            // patientId là bắt buộc trong request body
            if (request.getPatientId() == null) {
                return ResponseEntity.badRequest().body("patientId is required for staff/admin requests.");
            }
            patientIdToUse = request.getPatientId();
        } else {
            // TRƯỜNG HỢP 2: Bệnh nhân tự thực hiện
            // Lấy email từ token và gọi sang sys-srv để lấy patientId
            String patientEmail = authentication.getName();
            UserDto patient = userServiceClient.getUserByEmail(patientEmail);
            patientIdToUse = patient.id();
        }

        // Gọi service với patientId đã được xác định
        ProtocolTracking tracking = protocolTrackingService.startProtocol(patientIdToUse, request.getProtocolId());
        return ResponseEntity.ok(tracking);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProtocolTrackingResponseDto>> getTrackingForPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(protocolTrackingService.getTrackingForPatient(patientId));
    }
}