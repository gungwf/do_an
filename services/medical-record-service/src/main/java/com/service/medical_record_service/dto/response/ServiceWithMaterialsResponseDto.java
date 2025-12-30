package com.service.medical_record_service.dto.response;

import com.service.medical_record_service.entity.Service;
import java.util.List;

public class ServiceWithMaterialsResponseDto {
    private Service service;
    private List<ServiceMaterialResponseDto> serviceMaterials;

    public ServiceWithMaterialsResponseDto(Service service, List<ServiceMaterialResponseDto> serviceMaterials) {
        this.service = service;
        this.serviceMaterials = serviceMaterials;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List<ServiceMaterialResponseDto> getServiceMaterials() {
        return serviceMaterials;
    }

    public void setServiceMaterials(List<ServiceMaterialResponseDto> serviceMaterials) {
        this.serviceMaterials = serviceMaterials;
    }
}
