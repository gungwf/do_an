package com.product_inventory_service.product_inventory_service.repository;

import com.product_inventory_service.product_inventory_service.entity.Inventory;
import com.product_inventory_service.product_inventory_service.entity.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {
    // Spring Data JPA đủ thông minh để hiểu và query theo thuộc tính lồng trong Id
    List<Inventory> findById_BranchId(UUID branchId);
}