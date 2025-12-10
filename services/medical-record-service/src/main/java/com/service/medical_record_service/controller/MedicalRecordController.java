package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.LockRequest;
import com.service.medical_record_service.dto.request.MedicalRecordRequest;
import com.service.medical_record_service.dto.request.UpdateMedicalRecordRequest;
import com.service.medical_record_service.entity.Enum.BillType;
import com.service.medical_record_service.dto.response.MedicalRecordDetailResponse;
import com.service.medical_record_service.entity.MedicalRecord;
import com.service.medical_record_service.exception.AppException;
import com.service.medical_record_service.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;
    private final com.service.medical_record_service.client.client.UserServiceClient userServiceClient;

    @PostMapping
    public ResponseEntity<MedicalRecord> createMedicalRecord(@RequestBody MedicalRecordRequest request) {
        MedicalRecord newRecord = medicalRecordService.createMedicalRecord(request);
        return ResponseEntity.ok(newRecord);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<MedicalRecordDetailResponse> getRecordByAppointmentId(@PathVariable UUID appointmentId) {
        try {
            return ResponseEntity.ok(medicalRecordService.getRecordDetailByAppointmentId(appointmentId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('doctor')")
    public ResponseEntity<?> lockMedicalRecord(
            @PathVariable UUID id,
            @RequestBody LockRequest request
    ) {
        try {
            return ResponseEntity.ok(medicalRecordService.lockMedicalRecord(id, request.getSignatureData()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicalRecordDetailResponse> getMedicalRecordDetails(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(medicalRecordService.getRecordDetailById(id));
        } catch (AppException ae) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{id}/calculate-total")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, BigDecimal>> getBillTotal(
        @PathVariable UUID id,
        @RequestParam("type") String typeStr // Nhận tham số type
    ) {
        try {
            BillType type = BillType.valueOf(typeStr.toUpperCase());
            BigDecimal total = medicalRecordService.calculateBillTotal(id, type);
            return ResponseEntity.ok(Map.of("totalAmount", total));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/trigger-deduct-stock")
    @PreAuthorize("isAuthenticated()") // Chỉ service nội bộ
    public ResponseEntity<Void> triggerDeductStock(@PathVariable UUID id) {
        medicalRecordService.triggerDeductStock(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/check-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkStock(@PathVariable UUID id) {
        var shortages = medicalRecordService.checkStock(id);
        if (shortages == null || shortages.isEmpty()) {
            return ResponseEntity.ok().body(java.util.List.of());
        }
        return ResponseEntity.status(409).body(shortages);
    }

    @GetMapping("/patient/me")
    @PreAuthorize("hasAuthority('patient')")
    public ResponseEntity<org.springframework.data.domain.Page<com.service.medical_record_service.dto.response.MedicalRecordWithAppointmentResponse>> getMyMedicalRecords(
        Authentication authentication,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") int size,
        @RequestParam(value = "sort", required = false) String sort
    ) {
        try {
            String email = authentication.getName();
            var user = userServiceClient.getUserByEmail(email);
            java.util.UUID patientId = user.id();
            var pageRes = medicalRecordService.getMedicalRecordsForPatient(patientId, page, size, sort);
            return ResponseEntity.ok(pageRes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(org.springframework.data.domain.Page.empty());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecord> updateMedicalRecord(
            @PathVariable UUID id,
            @RequestBody UpdateMedicalRecordRequest request
    ) {
        // (AppExceptionHandler sẽ bắt lỗi nếu có)
        MedicalRecord updatedRecord = medicalRecordService.updateMedicalRecord(id, request);
        return ResponseEntity.ok(updatedRecord);
    }
}