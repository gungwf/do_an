package com.service.medical_record_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "product-inventory-service")
public interface ProductInventoryClient {
    @GetMapping("/products/{id}")
    ProductDto getProductById(@PathVariable("id") UUID id);

    @PatchMapping("/inventory/deduct")
    void deductStock(DeductStockRequest request);

}