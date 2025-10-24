package com.service.appointment_service.client.client;

import com.service.appointment_service.client.dto.MedicalRecordDto;
import com.service.appointment_service.client.dto.ProtocolDto;
import com.service.appointment_service.client.dto.ServiceDto;
import com.service.appointment_service.client.dto.ServiceMaterialDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@FeignClient(name = "medical-record-service")
public interface MedicalServiceClient {
    @GetMapping("/services/{id}")
    ServiceDto getServiceById(@PathVariable("id") UUID id);

    @GetMapping("/service-materials/service/{serviceId}")
    List<ServiceMaterialDto> getMaterialsForService(@PathVariable("serviceId") UUID serviceId);

    @GetMapping("/protocols/{id}")
    ProtocolDto getProtocolById(@PathVariable("id") UUID id);

    @GetMapping("/medical-records/{id}/calculate-total")
    Map<String, BigDecimal> getBillTotal(@PathVariable("id") UUID id);

    @PostMapping("/medical-records/{id}/trigger-deduct-stock")
    void triggerDeductStock(@PathVariable("id") UUID id);

    // Và đảm bảo bạn có hàm này để lấy MedicalRecord ID
    @GetMapping("/medical-records/appointment/{appointmentId}")
    MedicalRecordDto getRecordByAppointmentId(@PathVariable("appointmentId") UUID appointmentId);
}