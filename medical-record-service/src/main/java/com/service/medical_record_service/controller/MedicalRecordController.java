package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.LockRequest;
import com.service.medical_record_service.dto.request.MedicalRecordRequest;
import com.service.medical_record_service.entity.MedicalRecord;
import com.service.medical_record_service.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;

    @PostMapping
    public ResponseEntity<?> createMedicalRecord(@RequestBody MedicalRecordRequest request) {
        try {
            return ResponseEntity.ok(medicalRecordService.createMedicalRecord(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
}