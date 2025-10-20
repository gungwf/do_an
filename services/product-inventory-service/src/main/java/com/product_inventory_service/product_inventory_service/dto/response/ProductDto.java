package com.product_inventory_service.product_inventory_service.dto.response;

import com.product_inventory_service.product_inventory_service.entity.Enum.ProductType;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String productName,
        BigDecimal price,
        ProductType productType
) {}