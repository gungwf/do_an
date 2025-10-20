package com.service.appointment_service.client.dto;

import java.util.UUID;

public record UserDto(UUID id, String fullName, String email, String role) {}
