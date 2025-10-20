package com.product_inventory_service.product_inventory_service.dto.response;
import java.util.UUID;

public record ProductSimpleDto(UUID id, String productName) {}