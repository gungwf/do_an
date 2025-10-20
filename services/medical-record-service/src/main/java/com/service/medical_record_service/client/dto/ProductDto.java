package com.service.medical_record_service.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(UUID id, String productName, BigDecimal price, ProductType productType) {}

