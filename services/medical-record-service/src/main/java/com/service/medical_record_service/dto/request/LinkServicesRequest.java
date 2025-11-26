package com.service.medical_record_service.dto.request;

import java.util.List;
import java.util.UUID;

public record LinkServicesRequest(List<UUID> serviceIds) {}
