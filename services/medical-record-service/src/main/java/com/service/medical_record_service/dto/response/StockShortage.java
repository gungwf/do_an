package com.service.medical_record_service.dto.response;

import java.util.UUID;

public record StockShortage(UUID productId, int required, int available) {}
