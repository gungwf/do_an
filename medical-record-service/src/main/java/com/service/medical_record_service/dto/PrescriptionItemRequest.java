package com.service.medical_record_service.dto;

import java.util.UUID;

// Dùng record cho ngắn gọn, nó tương đương một class với các trường final và getter
public record PrescriptionItemRequest(
        UUID productId,
        int quantity,
        String dosage
) {}
