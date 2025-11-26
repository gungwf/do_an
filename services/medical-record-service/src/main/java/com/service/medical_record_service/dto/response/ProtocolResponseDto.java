package com.service.medical_record_service.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProtocolResponseDto(
        UUID id,
        String protocolName,
        String description,
        Integer totalSessions,
        BigDecimal price,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
