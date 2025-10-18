package com.service.appointment_service.dto.response;

import java.util.UUID;

public record BranchDto(UUID id, String branchName, String address) {}
