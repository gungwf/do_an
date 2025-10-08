package com.service.appointment_service.dto;

import java.util.UUID;

public record BranchDto(UUID id, String branchName, String address) {}
