package com.service.medical_record_service.dto;

import java.util.UUID;

public record PrescriptionTemplateItemDto(UUID productId, int quantity, String dosage) {}
