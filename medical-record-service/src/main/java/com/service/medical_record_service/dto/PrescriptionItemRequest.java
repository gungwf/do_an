package com.service.medical_record_service.dto;

import java.util.UUID;

public record PrescriptionItemRequest(
        UUID productId,
        int quantity,
        String dosage
) {}
