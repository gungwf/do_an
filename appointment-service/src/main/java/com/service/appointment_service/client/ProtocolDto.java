package com.service.appointment_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record ProtocolDto(UUID id, String protocolName, Integer totalSessions, BigDecimal price) {}
