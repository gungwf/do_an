package com.product_inventory_service.product_inventory_service.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class DeductStockRequest {
    private UUID branchId;
    private UUID productId;
    private Integer quantityToDeduct;
}