package com.service.appointment_service.client.client;

import com.service.appointment_service.client.dto.DeductStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-inventory-service")
public interface ProductInventoryClient {
    @PatchMapping("/inventory/deduct")
    void deductStock(@RequestBody DeductStockRequest request);
}