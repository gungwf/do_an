package com.service.appointment_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceDto(UUID id, String serviceName, BigDecimal price, Integer durationMinutes) {}
