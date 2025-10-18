package com.service.appointment_service.client.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "service-service", url = "http://localhost:8081/api/services")
public interface ServiceClient {

    @GetMapping("/{id}/price")
    BigDecimal getServicePrice(@PathVariable("id") UUID serviceId);
}
