package com.service.appointment_service.dto;

import com.service.appointment_service.entity.Enum.ProtocolStatus;

import java.util.UUID;

public record ProtocolTrackingResponseDto(
        UUID trackingId, // ID của bản ghi theo dõi
        UUID protocolId, // ID của gói liệu trình
        String protocolName, // Tên của liệu trình
        Integer totalSessions,
        Integer completedSessions,
        ProtocolStatus status
) {}