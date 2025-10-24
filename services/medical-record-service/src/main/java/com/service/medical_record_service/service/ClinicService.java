package com.service.medical_record_service.service;

import com.service.medical_record_service.dto.request.ServiceMaterialRequest;
import com.service.medical_record_service.dto.response.ServiceMaterialResponseDto;
import com.service.medical_record_service.dto.response.ServiceSimpleDto;
import com.service.medical_record_service.entity.Service;
import com.service.medical_record_service.entity.ServiceMaterial;
import com.service.medical_record_service.entity.ServiceMaterialId;
import com.service.medical_record_service.exception.AppException;
import com.service.medical_record_service.exception.ERROR_CODE;
import com.service.medical_record_service.repository.ServiceMaterialRepository;
import com.service.medical_record_service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ClinicService {
    private final ServiceRepository serviceRepository;
    private final ServiceMaterialRepository serviceMaterialRepository;

    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Service createService(Service service) {
        return serviceRepository.save(service);
    }

    public Service getServiceById(UUID id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ERROR_CODE.SERVICE_NOT_FOUND));
    }

    public Service updateService(UUID id, Service serviceDetails) {
        Service existingService = getServiceById(id);

        existingService.setServiceName(serviceDetails.getServiceName());
        existingService.setDescription(serviceDetails.getDescription());
        existingService.setPrice(serviceDetails.getPrice());
        existingService.setActive(serviceDetails.isActive());

        return serviceRepository.save(existingService);
    }

    public void deleteService(UUID id) {
        Service existingService = getServiceById(id);
        existingService.setActive(false);

        serviceRepository.save(existingService);
    }

    public ServiceMaterial addMaterialToService(ServiceMaterialRequest request) {
        ServiceMaterialId id = new ServiceMaterialId();
        id.setServiceId(request.getServiceId());
        id.setProductId(request.getProductId());

        ServiceMaterial material = new ServiceMaterial();
        material.setId(id);
        material.setQuantityConsumed(request.getQuantityConsumed());

        return serviceMaterialRepository.save(material);
    }

    public List<ServiceMaterialResponseDto> getMaterialsForService(UUID serviceId) {
        List<ServiceMaterial> materials = serviceMaterialRepository.findById_ServiceId(serviceId);
        return materials.stream()
                .map(material -> new ServiceMaterialResponseDto(
                        material.getId().getProductId(),
                        material.getQuantityConsumed()
                ))
                .collect(Collectors.toList());
    }

    public List<ServiceSimpleDto> getAllServicesSimple() {
        return serviceRepository.findAll()
                .stream()
                .filter(Service::isActive)
                .map(service -> new ServiceSimpleDto(service.getId(), service.getServiceName()))
                .collect(Collectors.toList());
    }
}