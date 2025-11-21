package com.service.medical_record_service.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineRequest(UUID productId, int quantity, BigDecimal unitPrice) {}
