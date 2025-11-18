package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.LockRequest;
import com.service.medical_record_service.dto.request.MedicalRecordRequest;
import com.service.medical_record_service.dto.request.UpdateMedicalRecordRequest;
import com.service.medical_record_service.entity.MedicalRecord;
import com.service.medical_record_service.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping
    public ResponseEntity<MedicalRecord> createMedicalRecord(@RequestBody MedicalRecordRequest request) {
        MedicalRecord newRecord = medicalRecordService.createMedicalRecord(request);
        return ResponseEntity.ok(newRecord);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<MedicalRecord> getRecordByAppointmentId(@PathVariable UUID appointmentId) {
        try {
            return ResponseEntity.ok(medicalRecordService.getRecordByAppointmentId(appointmentId));
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

    @GetMapping("/{id}/calculate-total")
    @PreAuthorize("isAuthenticated()") // Bảo vệ, chỉ service nội bộ (có token) mới được gọi
    public ResponseEntity<Map<String, BigDecimal>> getBillTotal(@PathVariable UUID id) {
        BigDecimal total = medicalRecordService.calculateBillTotal(id);
        return ResponseEntity.ok(Map.of("totalAmount", total));
    }

    @PostMapping("/{id}/trigger-deduct-stock")
    @PreAuthorize("isAuthenticated()") // Chỉ service nội bộ
    public ResponseEntity<Void> confirmPaymentAndDeductStock(@PathVariable UUID id) {
        medicalRecordService.triggerDeductStock(id);
        return ResponseEntity.ok().build();
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