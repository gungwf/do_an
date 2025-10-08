package com.service.appointment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.UUID;


@FeignClient(name = "medical-record-service")
public interface MedicalServiceClient {
    @GetMapping("/services/{id}")
    ServiceDto getServiceById(@PathVariable("id") UUID id);

    @GetMapping("/service-materials/service/{serviceId}")
    List<ServiceMaterialDto> getMaterialsForService(@PathVariable("serviceId") UUID serviceId);


}