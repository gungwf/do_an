package com.product_inventory_service.product_inventory_service.controller;

import com.product_inventory_service.product_inventory_service.dto.DeductStockRequest;
import com.product_inventory_service.product_inventory_service.dto.UpdateStockRequest;
import com.product_inventory_service.product_inventory_service.entity.Inventory;
import com.product_inventory_service.product_inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<Inventory>> getInventoryByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(inventoryService.getInventoryByBranch(branchId));
    }

    @PatchMapping("/stock")
    public ResponseEntity<Inventory> updateStock(@RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(inventoryService.updateStock(
                request.getBranchId(),
                request.getProductId(),
                request.getQuantityChange()
        ));
    }

    @PatchMapping("/deduct")
    public ResponseEntity<?> deductStock(@RequestBody DeductStockRequest request) {
        try {
            return ResponseEntity.ok(inventoryService.deductStock(request));
        } catch (Exception e) {
            // Trả về 400 Bad Request nếu có lỗi nghiệp vụ (vd: hết hàng)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //test
    @GetMapping("/check/{branchId}/{productId}")
    public ResponseEntity<String> checkInventory(
            @PathVariable UUID branchId,
            @PathVariable UUID productId
    ) {
        boolean exists = inventoryService.checkInventoryExists(branchId, productId);
        if (exists) {
            return ResponseEntity.ok("TÌM THẤY bản ghi tồn kho cho branchId: " + branchId + " và productId: " + productId);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND) // Trả về 404
                    .body("KHÔNG TÌM THẤY bản ghi tồn kho cho branchId: " + branchId + " và productId: " + productId);
        }
    }
}