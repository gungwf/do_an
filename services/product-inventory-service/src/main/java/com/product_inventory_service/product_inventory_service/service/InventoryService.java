package com.product_inventory_service.product_inventory_service.service;

import com.product_inventory_service.product_inventory_service.dto.request.DeductStockRequest;
import com.product_inventory_service.product_inventory_service.entity.Inventory;
import com.product_inventory_service.product_inventory_service.entity.InventoryId;
import com.product_inventory_service.product_inventory_service.exception.AppException;
import com.product_inventory_service.product_inventory_service.exception.ERROR_CODE;
import com.product_inventory_service.product_inventory_service.repository.InventoryRepository;
import com.product_inventory_service.product_inventory_service.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository; // Inject ProductRepository

    public List<Inventory> getInventoryByBranch(UUID branchId) {
        return inventoryRepository.findById_BranchId(branchId);
    }

    public Inventory updateStock(UUID branchId, UUID productId, Integer quantityChange) {
        // Kiểm tra Product và Branch có tồn tại không
        productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        // (Tương tự, bạn sẽ gọi Feign client để kiểm tra Branch)

        InventoryId inventoryId = new InventoryId();
        inventoryId.setBranchId(branchId);
        inventoryId.setProductId(productId);

        // Tìm bản ghi tồn kho, nếu chưa có thì tạo mới
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElse(new Inventory());

        if (inventory.getId() == null) {
            inventory.setId(inventoryId);
            inventory.setQuantity(0);
        }

        // Cập nhật số lượng
        int newQuantity = inventory.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new RuntimeException("Stock cannot be negative.");
        }
        inventory.setQuantity(newQuantity);

        return inventoryRepository.save(inventory);
    }

    @Transactional // Đảm bảo các thao tác DB trong hàm này là một khối duy nhất
    public Inventory deductStock(DeductStockRequest request) {
        // 1. Xác định ID của bản ghi tồn kho

        System.out.println("--- DEBUG: BÊN NHẬN (product-inventory-service) ---");
        System.out.println("Đã nhận yêu cầu trừ kho cho Product ID: " + request.getProductId());
        System.out.println("Đã nhận yêu cầu trừ kho cho Branch ID: " + request.getBranchId());

        InventoryId inventoryId = new InventoryId();
        inventoryId.setBranchId(request.getBranchId());
        inventoryId.setProductId(request.getProductId());

        // 2. Lấy thông tin tồn kho, nếu không có thì báo lỗi
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AppException(ERROR_CODE.INVENTORY_NOT_FOUND));

        // 3. Kiểm tra số lượng
        if (inventory.getQuantity() < request.getQuantityToDeduct()) {
            throw new AppException(ERROR_CODE.INSUFFICIENT_STOCK);
        }

        // 4. Trừ kho và lưu lại
        inventory.setQuantity(inventory.getQuantity() - request.getQuantityToDeduct());
        return inventoryRepository.save(inventory);
    }

    //test
    public boolean checkInventoryExists(UUID branchId, UUID productId) {
        InventoryId inventoryId = new InventoryId();
        inventoryId.setBranchId(branchId);
        inventoryId.setProductId(productId);

        // Trả về true nếu tìm thấy, false nếu không
        return inventoryRepository.findById(inventoryId).isPresent();
    }


}