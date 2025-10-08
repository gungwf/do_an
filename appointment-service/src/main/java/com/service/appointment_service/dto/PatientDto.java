package com.service.appointment_service.dto;

import java.util.UUID;

public record PatientDto(UUID id, String fullName, String email) {}
