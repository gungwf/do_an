package com.service.appointment_service.client.dto;

import java.util.UUID;

//record ServiceDto(UUID id, String serviceName, BigDecimal price, Integer durationMinutes) {}
public record ServiceMaterialDto(UUID productId, Integer quantityConsumed) {}
