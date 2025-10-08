package com.service.medical_record_service.client;

import lombok.Data;
import java.util.UUID;

@Data
public class DeductStockRequest {
    private UUID branchId;
    private UUID productId;
    private Integer quantityToDeduct;
}