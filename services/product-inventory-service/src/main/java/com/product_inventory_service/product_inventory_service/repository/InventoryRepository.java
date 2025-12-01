package com.product_inventory_service.product_inventory_service.repository;

import com.product_inventory_service.product_inventory_service.entity.Inventory;
import com.product_inventory_service.product_inventory_service.entity.InventoryId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {
    // Spring Data JPA đủ thông minh để hiểu và query theo thuộc tính lồng trong Id
    List<Inventory> findById_BranchId(UUID branchId);

    // Phân trang theo chi nhánh
    Page<Inventory> findById_BranchId(UUID branchId, Pageable pageable);

    // Lọc theo khoảng số lượng
    Page<Inventory> findById_BranchIdAndQuantityBetween(UUID branchId, Integer min, Integer max, Pageable pageable);

    // Lọc theo số lượng nhỏ hơn ngưỡng
    Page<Inventory> findById_BranchIdAndQuantityLessThan(UUID branchId, Integer threshold, Pageable pageable);

        // Toàn bộ tồn kho thấp dưới ngưỡng (không lọc theo chi nhánh)
        Page<Inventory> findByQuantityLessThan(Integer threshold, Pageable pageable);

    // Lọc theo productId cụ thể (hữu ích khi gom trong cùng endpoint)
    Page<Inventory> findById_BranchIdAndId_ProductId(UUID branchId, UUID productId, Pageable pageable);
}