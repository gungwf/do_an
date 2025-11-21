package com.service.appointment_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineRequestDto(UUID productId, int quantity, BigDecimal unitPrice) {}
