package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.ServiceMaterialRequest;
import com.service.medical_record_service.dto.ServiceMaterialResponseDto;
import com.service.medical_record_service.entity.ServiceMaterial;
import com.service.medical_record_service.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/service-materials")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ServiceMaterialController {

    private final ClinicService clinicService;

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<ServiceMaterial> addMaterialToService(@RequestBody ServiceMaterialRequest request) {
        return ResponseEntity.ok(clinicService.addMaterialToService(request));
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceMaterialResponseDto>> getMaterialsForService(@PathVariable UUID serviceId) { // <-- SỬA Ở ĐÂY
        return ResponseEntity.ok(clinicService.getMaterialsForService(serviceId));
    }
}