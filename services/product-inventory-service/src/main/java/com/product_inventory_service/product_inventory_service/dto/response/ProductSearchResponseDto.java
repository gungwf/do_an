package com.product_inventory_service.product_inventory_service.dto.response;

import com.product_inventory_service.product_inventory_service.entity.Enum.ProductType;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductSearchResponseDto {
    private UUID id;
    private String productName;
    private String description;
    private BigDecimal price;
    private ProductType productType;
    private String category;
    private String imageUrl;
    private boolean isActive;
}