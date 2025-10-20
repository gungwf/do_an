package com.service.medical_record_service.client.dto;

import java.util.UUID;

public record BranchDto(UUID id, String branchName, String address) {}
