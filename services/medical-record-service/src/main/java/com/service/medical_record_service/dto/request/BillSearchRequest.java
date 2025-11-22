package com.service.medical_record_service.dto.request;

import java.util.UUID;

public record BillSearchRequest(
        String billType,
        String status,
        UUID branchId,
        Long fromPaidAt,
        Long toPaidAt,
        String patientName,
        Integer page,
        Integer size
) {}
