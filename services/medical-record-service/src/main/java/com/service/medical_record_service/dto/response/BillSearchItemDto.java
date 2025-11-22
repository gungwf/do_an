package com.service.medical_record_service.dto.response;

import com.service.medical_record_service.entity.Enum.BillStatus;
import com.service.medical_record_service.entity.Enum.BillType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillSearchItemDto(
        UUID billId,
        BillType billType,
        BillStatus status,
        UUID branchId,
        UUID patientId,
        String patientName,
        BigDecimal totalAmount,
        Instant paidAt,
        Instant createdAt
) {}
