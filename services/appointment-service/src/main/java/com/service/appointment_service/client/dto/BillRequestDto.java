package com.service.appointment_service.client.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillRequestDto(
    UUID patientId,
    UUID creatorId,
    UUID branchId,
    String billType,
    BigDecimal totalAmount,
    List<BillLineRequestDto> items,
    String note
) {}
