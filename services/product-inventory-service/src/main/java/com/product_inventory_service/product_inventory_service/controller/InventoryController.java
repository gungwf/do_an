package com.product_inventory_service.product_inventory_service.controller;

import com.product_inventory_service.product_inventory_service.dto.request.DeductStockRequest;
import com.product_inventory_service.product_inventory_service.dto.request.UpdateStockRequest;
import com.product_inventory_service.product_inventory_service.dto.request.InventorySearchRequest;
import com.product_inventory_service.product_inventory_service.dto.request.LowStockSearchRequest;
import com.product_inventory_service.product_inventory_service.entity.Inventory;
import com.product_inventory_service.product_inventory_service.service.InventoryService;
import com.product_inventory_service.product_inventory_service.dto.response.ApiResponse;
import com.product_inventory_service.product_inventory_service.dto.response.InventoryResponseDto;
import com.product_inventory_service.product_inventory_service.dto.response.InventoryCheckResponseDto;
import com.product_inventory_service.product_inventory_service.repository.ProductRepository;
import com.product_inventory_service.product_inventory_service.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import com.product_inventory_service.product_inventory_service.service.JwtService;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final JwtService jwtService;

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("admin".equalsIgnoreCase(ga.getAuthority()) || "ROLE_ADMIN".equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private UUID getPrincipalBranchId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtService.extractBranchId(token);
        }
        return null;
    }


        @PatchMapping("/stock")
        public ResponseEntity<ApiResponse<InventoryResponseDto>> updateStock(@RequestBody UpdateStockRequest request, HttpServletRequest httpReq) {
        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null) {
                request.setBranchId(principalBranch);
            }
        }
        var inventory = inventoryService.updateStock(
            request.getBranchId(),
            request.getProductId(),
            request.getQuantityChange()
        );
        String productName = productRepository.findById(inventory.getId().getProductId())
                .map(Product::getProductName).orElse(null);
        var dto = new InventoryResponseDto(
            inventory.getId().getBranchId(),
            inventory.getId().getProductId(),
            productName,
            productRepository.findById(inventory.getId().getProductId()).map(Product::getImageUrl).orElse(null),
            inventory.getQuantity()
        );
        var resp = ApiResponse.<InventoryResponseDto>builder()
            .code(0)
            .message("OK")
            .result(dto)
            .build();
        return ResponseEntity.ok(resp);
        }

    @PatchMapping("/deduct")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> deductStock(@RequestBody DeductStockRequest request, HttpServletRequest httpReq) {
        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null) {
                request.setBranchId(principalBranch);
            }
        }
        var inventory = inventoryService.deductStock(request);
        String productName = productRepository.findById(inventory.getId().getProductId())
            .map(Product::getProductName).orElse(null);
        var dto = new InventoryResponseDto(
            inventory.getId().getBranchId(),
            inventory.getId().getProductId(),
            productName,
            productRepository.findById(inventory.getId().getProductId()).map(Product::getImageUrl).orElse(null),
            inventory.getQuantity()
        );
        var resp = ApiResponse.<InventoryResponseDto>builder()
                .code(0)
                .message("OK")
                .result(dto)
                .build();
        return ResponseEntity.ok(resp);
    }

        // POST search endpoint: pagination & filters in body
        @PostMapping("/branch/{branchId}/search")
        public ResponseEntity<ApiResponse<Page<InventoryResponseDto>>> searchInventoryByBranch(
            @PathVariable UUID branchId,
            @RequestBody InventorySearchRequest request,
            HttpServletRequest httpReq
        ) {
        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null && !principalBranch.equals(branchId)) {
                ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
                        .code(403)
                        .message("Forbidden: cannot access other branch")
                        .build();
                return ResponseEntity.status(403).body(resp);
            }
        }
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "lastUpdatedAt";
        String sortDir = request.getSortDir() != null ? request.getSortDir() : "DESC";

        Sort sort = Sort.by(sortBy);
        sort = "DESC".equalsIgnoreCase(sortDir) ? sort.descending() : sort.ascending();

        List<UUID> filterProductIds = null;
        boolean hasNameFilter = request.getProductName() != null && !request.getProductName().isBlank();
        if (hasNameFilter) {
            var matches = productRepository.findByProductNameContainingIgnoreCase(request.getProductName());
            filterProductIds = matches.stream().map(Product::getId).collect(Collectors.toList());
            if (filterProductIds.isEmpty()) {
            Page<InventoryResponseDto> empty = Page.empty();
            ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
                .code(0)
                .message("OK")
                .result(empty)
                .build();
            return ResponseEntity.ok(resp);
            }
        }

        Page<Inventory> pageData = (filterProductIds != null)
            ? inventoryService.searchInventoryByBranchWithProductIds(
                branchId,
                request.getMinQuantity(),
                request.getMaxQuantity(),
                request.getLowStockOnly(),
                filterProductIds,
                page,
                size,
                sort
            )
            : inventoryService.searchInventoryByBranch(
                branchId,
                request.getMinQuantity(),
                request.getMaxQuantity(),
                request.getLowStockOnly(),
                null,
                page,
                size,
                sort
            );

        Page<InventoryResponseDto> dtoPage = pageData.map(inv -> {
            var pid = inv.getId().getProductId();
            String pName = productRepository.findById(pid).map(Product::getProductName).orElse(null);
            return new InventoryResponseDto(
                inv.getId().getBranchId(),
                pid,
                pName,
                productRepository.findById(pid).map(Product::getImageUrl).orElse(null),
                inv.getQuantity()
            );
        });

        ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
            .code(0)
            .message("OK")
            .result(dtoPage)
            .build();
        return ResponseEntity.ok(resp);
        }

    // Low stock warning endpoint (global or filtered by branch)
    @PostMapping("/low-stock/search")
    public ResponseEntity<ApiResponse<Page<InventoryResponseDto>>> searchLowStock(@RequestBody LowStockSearchRequest request, HttpServletRequest httpReq) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "lastUpdatedAt";
        String sortDir = request.getSortDir() != null ? request.getSortDir() : "ASC"; // thường xem hàng thiếu tăng dần theo cập nhật

        Sort sort = Sort.by(sortBy);
        sort = "DESC".equalsIgnoreCase(sortDir) ? sort.descending() : sort.ascending();

        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null) {
                request.setBranchId(principalBranch);
            }
        }

        Page<Inventory> pageData = inventoryService.searchLowStock(request.getBranchId(), page, size, sort);

        Page<InventoryResponseDto> dtoPage = pageData.map(inv -> {
            var pid = inv.getId().getProductId();
            String pName = productRepository.findById(pid).map(Product::getProductName).orElse(null);
                return new InventoryResponseDto(
                    inv.getId().getBranchId(),
                    pid,
                    pName,
                    productRepository.findById(pid).map(Product::getImageUrl).orElse(null),
                    inv.getQuantity()
                );
        });

        ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
                .code(0)
                .message("OK")
                .result(dtoPage)
                .build();
        return ResponseEntity.ok(resp);
    }

    // Convenience endpoint: staff doesn't need to pass branchId in path
    @PostMapping("/branch/search")
    public ResponseEntity<ApiResponse<Page<InventoryResponseDto>>> searchInventoryByBranchNoPath(
            @RequestBody InventorySearchRequest request,
            HttpServletRequest httpReq
    ) {
        UUID effectiveBranchId = request.getBranchId();
        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null) {
                effectiveBranchId = principalBranch;
            }
        }
        if (effectiveBranchId == null) {
            ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
                    .code(400)
                    .message("Bad Request: branchId required (admin may provide in body; staff taken from token)")
                    .build();
            return ResponseEntity.badRequest().body(resp);
        }

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "lastUpdatedAt";
        String sortDir = request.getSortDir() != null ? request.getSortDir() : "DESC";

        Sort sort = Sort.by(sortBy);
        sort = "DESC".equalsIgnoreCase(sortDir) ? sort.descending() : sort.ascending();

        List<UUID> filterProductIds2 = null;
        boolean hasNameFilter2 = request.getProductName() != null && !request.getProductName().isBlank();
        if (hasNameFilter2) {
            var matches2 = productRepository.findByProductNameContainingIgnoreCase(request.getProductName());
            filterProductIds2 = matches2.stream().map(Product::getId).collect(Collectors.toList());
            if (filterProductIds2.isEmpty()) {
            Page<InventoryResponseDto> empty = Page.empty();
            ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
                .code(0)
                .message("OK")
                .result(empty)
                .build();
            return ResponseEntity.ok(resp);
            }
        }

        Page<Inventory> pageData = (filterProductIds2 != null)
            ? inventoryService.searchInventoryByBranchWithProductIds(
                effectiveBranchId,
                request.getMinQuantity(),
                request.getMaxQuantity(),
                request.getLowStockOnly(),
                filterProductIds2,
                page,
                size,
                sort
            )
            : inventoryService.searchInventoryByBranch(
                effectiveBranchId,
                request.getMinQuantity(),
                request.getMaxQuantity(),
                request.getLowStockOnly(),
                null,
                page,
                size,
                sort
            );

        Page<InventoryResponseDto> dtoPage = pageData.map(inv -> {
            var pid = inv.getId().getProductId();
            String pName = productRepository.findById(pid).map(Product::getProductName).orElse(null);
                return new InventoryResponseDto(
                    inv.getId().getBranchId(),
                    pid,
                    pName,
                    productRepository.findById(pid).map(Product::getImageUrl).orElse(null),
                    inv.getQuantity()
                );
        });

        ApiResponse<Page<InventoryResponseDto>> resp = ApiResponse.<Page<InventoryResponseDto>>builder()
                .code(0)
                .message("OK")
                .result(dtoPage)
                .build();
        return ResponseEntity.ok(resp);
    }

    //test
    @GetMapping("/check/{branchId}/{productId}")
    public ResponseEntity<ApiResponse<InventoryCheckResponseDto>> checkInventory(
            @PathVariable UUID branchId,
            @PathVariable UUID productId,
            HttpServletRequest httpReq
    ) {
        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null && !principalBranch.equals(branchId)) {
                ApiResponse<InventoryCheckResponseDto> resp = ApiResponse.<InventoryCheckResponseDto>builder()
                        .code(403)
                        .message("Forbidden: cannot access other branch")
                        .build();
                return ResponseEntity.status(403).body(resp);
            }
        }
        boolean exists = inventoryService.checkInventoryExists(branchId, productId);
        Integer quantity = null;
        if (exists) {
            var inv = inventoryService.getInventoryByBranchAndProduct(branchId, productId);
            quantity = inv.getQuantity();
        }
        var dto = new InventoryCheckResponseDto(branchId, productId, exists, quantity);
        var resp = ApiResponse.<InventoryCheckResponseDto>builder()
                .code(0)
                .message("OK")
                .result(dto)
                .build();
        return ResponseEntity.ok(resp);
    }

        @GetMapping("/{branchId}/{productId}")
        public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventoryByBranchAndProduct(
            @PathVariable UUID branchId,
            @PathVariable UUID productId,
            HttpServletRequest httpReq
        ) {
        if (!isAdmin()) {
            UUID principalBranch = getPrincipalBranchId(httpReq);
            if (principalBranch != null && !principalBranch.equals(branchId)) {
                ApiResponse<InventoryResponseDto> resp = ApiResponse.<InventoryResponseDto>builder()
                        .code(403)
                        .message("Forbidden: cannot access other branch")
                        .build();
                return ResponseEntity.status(403).body(resp);
            }
        }
        var inventory = inventoryService.getInventoryByBranchAndProduct(branchId, productId);
        String productName2 = productRepository.findById(inventory.getId().getProductId())
                .map(Product::getProductName).orElse(null);
        var dto = new InventoryResponseDto(
            inventory.getId().getBranchId(),
            inventory.getId().getProductId(),
            productName2,
            productRepository.findById(inventory.getId().getProductId()).map(Product::getImageUrl).orElse(null),
            inventory.getQuantity()
        );
        var resp = ApiResponse.<InventoryResponseDto>builder()
            .code(0)
            .message("OK")
            .result(dto)
            .build();
        return ResponseEntity.ok(resp);
        }
}