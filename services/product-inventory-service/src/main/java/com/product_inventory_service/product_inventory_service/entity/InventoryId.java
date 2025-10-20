package com.product_inventory_service.product_inventory_service.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class InventoryId implements Serializable {
    private UUID productId;
    private UUID branchId;
}