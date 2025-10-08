package com.service.medical_record_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ServiceMaterialRequest {
    private UUID serviceId;
    private UUID productId;
    private Integer quantityConsumed;
}