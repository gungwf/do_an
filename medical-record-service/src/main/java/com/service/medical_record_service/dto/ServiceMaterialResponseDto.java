package com.service.medical_record_service.dto;

import java.util.UUID;

public record ServiceMaterialResponseDto(
        UUID productId,
        Integer quantityConsumed
) {}