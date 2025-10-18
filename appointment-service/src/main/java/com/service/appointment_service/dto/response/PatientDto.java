package com.service.appointment_service.dto.response;

import java.util.UUID;

public record PatientDto(UUID id, String fullName, String email) {}
