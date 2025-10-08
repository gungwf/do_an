package com.product_inventory_service.product_inventory_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateStockRequest {
    private UUID branchId;
    private UUID productId;
    private Integer quantityChange; // Số lượng thay đổi (+ là nhập, - là xuất)
}