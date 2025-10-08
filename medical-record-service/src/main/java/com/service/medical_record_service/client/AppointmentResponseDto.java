package com.service.medical_record_service.client;

import java.util.UUID;

public record AppointmentResponseDto(UUID id, BranchDto branch) {}

