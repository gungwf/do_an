package com.service.appointment_service.client.dto;

import java.util.UUID;

public record StockShortageDto(UUID productId, int required, int available) {}
