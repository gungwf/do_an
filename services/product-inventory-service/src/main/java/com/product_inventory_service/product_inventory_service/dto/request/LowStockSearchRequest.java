package com.product_inventory_service.product_inventory_service.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class LowStockSearchRequest {
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
    private UUID branchId; // optional branch filter
}
