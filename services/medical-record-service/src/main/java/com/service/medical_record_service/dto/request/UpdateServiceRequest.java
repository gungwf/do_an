package com.service.medical_record_service.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateServiceRequest {
    private String serviceName;
    private String description;
    private BigDecimal price;
    private Boolean active;
    private List<MaterialUpdateDto> materials;

    @Data
    public static class MaterialUpdateDto {
        private UUID productId;
        private Integer quantityConsumed;
    }
}
