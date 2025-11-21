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
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "medical-record-service")
public interface MedicalServiceClient {
    @GetMapping("/services/{id}")
    ServiceDto getServiceById(@PathVariable("id") UUID id);

    @GetMapping("/service-materials/service/{serviceId}")
    List<ServiceMaterialDto> getMaterialsForService(@PathVariable("serviceId") UUID serviceId);

    @GetMapping("/protocols/{id}")
    ProtocolDto getProtocolById(@PathVariable("id") UUID id);

    @GetMapping("/medical-records/{id}/calculate-total")
    Map<String, BigDecimal> getBillTotal(
        @PathVariable("id") UUID id,
        @RequestParam("type") String type // SERVICE_PAYMENT hoặc DRUG_PAYMENT
    );

    @PostMapping("/medical-records/{id}/trigger-deduct-stock")
    void triggerDeductStock(@PathVariable("id") UUID id);

    // Và đảm bảo bạn có hàm này để lấy MedicalRecord ID
    @GetMapping("/medical-records/appointment/{appointmentId}")
    MedicalRecordDto getRecordByAppointmentId(@PathVariable("appointmentId") UUID appointmentId);

    @GetMapping("/medical-records/{id}/check-stock")
    java.util.List<com.service.appointment_service.client.dto.StockShortageDto> checkStock(@PathVariable("id") UUID id);

    @PostMapping("/bills")
    java.util.Map<String, Object> createBill(@org.springframework.web.bind.annotation.RequestBody com.service.appointment_service.client.dto.BillRequestDto request);

    @PostMapping("/bills/prod")
    java.util.Map<String, Object> createBillProducts(@org.springframework.web.bind.annotation.RequestBody com.service.appointment_service.client.dto.BillRequestDto request);

    @PostMapping("/bills/{id}/mark-paid")
    void markBillPaid(@PathVariable("id") UUID id);

    @GetMapping("/bills/{id}")
    java.util.Map<String, Object> getBillById(@PathVariable("id") UUID id);

    @GetMapping("/bills/{id}/check-stock")
    java.util.List<com.service.appointment_service.client.dto.StockShortageDto> checkStockForBill(@PathVariable("id") UUID id);
}