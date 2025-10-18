package com.service.medical_record_service.client.dto;

import java.util.UUID;

public record AppointmentResponseDto(UUID id, BranchDto branch) {}

