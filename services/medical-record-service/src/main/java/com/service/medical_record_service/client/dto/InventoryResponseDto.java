package com.service.medical_record_service.client.dto;

import java.util.UUID;
public record InventoryResponseDto(UUID branchId, UUID productId, Integer quantity) {}
