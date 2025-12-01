package com.product_inventory_service.product_inventory_service.dto.response;

import java.util.UUID;

public record InventoryCheckResponseDto(UUID branchId, UUID productId, boolean exists, Integer quantity) {}
