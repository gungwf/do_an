package com.product_inventory_service.product_inventory_service.service;

import com.product_inventory_service.product_inventory_service.entity.Inventory;
import com.product_inventory_service.product_inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryScheduler {

    private final InventoryRepository inventoryRepository;
    @Value("${inventory.warning.threshold}")
    private int stockThreshold;

    @Scheduled(cron = "0 0 */2 * * *")
    public void checkLowStockLevels() {
        log.info("--- [SCHEDULER] Bắt đầu quét cảnh báo tồn kho thấp ---");

        // 1. Lấy tất cả các bản ghi tồn kho
        List<Inventory> allInventory = inventoryRepository.findAll();

        // 2. Lặp qua và kiểm tra
        for (Inventory item : allInventory) {
            if (item.getQuantity() < stockThreshold) {
                // Nếu số lượng thấp hơn ngưỡng, in ra một cảnh báo
                // Trong thực tế, bạn có thể gửi email/thông báo cho quản lý ở đây
                log.warn("!!! CẢNH BÁO TỒN KHO THẤP !!! - Sản phẩm ID: {} tại Chi nhánh ID: {} chỉ còn {} sản phẩm.",
                        item.getId().getProductId(),
                        item.getId().getBranchId(),
                        item.getQuantity());
            }
        }

        log.info("--- [SCHEDULER] Hoàn thành quét cảnh báo tồn kho ---");
    }
}