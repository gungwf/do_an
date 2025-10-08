package com.service.appointment_service.client;

import java.util.UUID;

//record ServiceDto(UUID id, String serviceName, BigDecimal price, Integer durationMinutes) {}
public record ServiceMaterialDto(UUID productId, Integer quantityConsumed) {}
