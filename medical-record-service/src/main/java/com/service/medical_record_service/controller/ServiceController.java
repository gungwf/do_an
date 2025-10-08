package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.ServiceSimpleDto;
import com.service.medical_record_service.entity.Service;
import com.service.medical_record_service.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ServiceController {
    private final ClinicService clinicService;

    @PostMapping
    public ResponseEntity<Service> createService(@RequestBody Service service) {
        return ResponseEntity.ok(clinicService.createService(service));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Service>> getAllServices() {
        return ResponseEntity.ok(clinicService.getAllServices());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Service> getServiceById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(clinicService.getServiceById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Service> updateService(@PathVariable UUID id, @RequestBody Service serviceDetails) {
        try {
            return ResponseEntity.ok(clinicService.updateService(id, serviceDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        try {
            clinicService.deleteService(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/simple")
    @PreAuthorize("permitAll()") // Cho phép tất cả mọi người xem danh sách dịch vụ
    public ResponseEntity<List<ServiceSimpleDto>> getAllServicesSimple() {
        return ResponseEntity.ok(clinicService.getAllServicesSimple());
    }
}