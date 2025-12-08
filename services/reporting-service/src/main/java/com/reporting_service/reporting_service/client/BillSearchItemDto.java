package com.reporting_service.reporting_service.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillSearchItemDto(
        UUID billId,
        String billType,
        String status,
        UUID branchId,
        UUID patientId,
        String patientName,
        BigDecimal totalAmount,
        Instant paidAt,
        Instant createdAt
) {}
