package com.service.medical_record_service.client.dto;

import java.util.UUID;

public record PatientDto(UUID id, String fullName, String email) {}
