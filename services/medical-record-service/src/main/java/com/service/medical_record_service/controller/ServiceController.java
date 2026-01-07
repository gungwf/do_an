package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.ServiceMaterialRequest;
import com.service.medical_record_service.dto.request.ServiceSearchRequest;
import com.service.medical_record_service.dto.request.UpdateServiceRequest;
import com.service.medical_record_service.dto.response.ServiceSimpleDto;
import com.service.medical_record_service.dto.response.ServiceWithMaterialsResponseDto;
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
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Service> createService(@RequestBody Service service) {
        return ResponseEntity.ok(clinicService.createService(service));
    }

        @PostMapping("/search")
        public ResponseEntity<List<Service>> getAllServices(@RequestBody ServiceSearchRequest request) {
        return ResponseEntity.ok(
            clinicService.getAllServices(request.getPage(), request.getSize(), request.getSearch(), request.getSortBy(), request.getSortDir())
        );
        }

    @GetMapping("/{id}")
    public ResponseEntity<?> getServiceById(@PathVariable UUID id) {
        try {
            Service service = clinicService.getServiceById(id);
            var materials = clinicService.getMaterialsForService(id);
            return ResponseEntity.ok(new ServiceWithMaterialsResponseDto(service, materials));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<?> updateService(@PathVariable UUID id, @RequestBody UpdateServiceRequest request) {
        try {
            Service serviceDetails = new Service();
            serviceDetails.setServiceName(request.getServiceName());
            serviceDetails.setDescription(request.getDescription());
            serviceDetails.setPrice(request.getPrice());
            serviceDetails.setActive(request.getActive() != null ? request.getActive() : true);
            Service updatedService = clinicService.updateService(id, serviceDetails);
            if (request.getMaterials() != null) {
                for (var m : request.getMaterials()) {
                    var materialRequest = new ServiceMaterialRequest();
                    materialRequest.setServiceId(id);
                    materialRequest.setProductId(m.getProductId());
                    materialRequest.setQuantityConsumed(m.getQuantityConsumed());
                    clinicService.updateMaterialInService(materialRequest);
                }
            }
            var materials = clinicService.getMaterialsForService(id);
            return ResponseEntity.ok(new ServiceWithMaterialsResponseDto(updatedService, materials));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
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