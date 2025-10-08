package com.service.medical_record_service.client;

import java.util.UUID;

public record BranchDto(UUID id, String branchName, String address) {}
