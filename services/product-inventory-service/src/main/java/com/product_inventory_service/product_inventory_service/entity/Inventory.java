package com.product_inventory_service.product_inventory_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "inventory")
@Data
public class Inventory {

    @EmbeddedId
    private InventoryId id;

    @Column(nullable = false)
    private Integer quantity;

    private LocalDate expiryDate;

    @UpdateTimestamp
    private Instant lastUpdatedAt;
}