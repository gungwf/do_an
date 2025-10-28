package com.product_inventory_service.product_inventory_service.controller;

import com.product_inventory_service.product_inventory_service.dto.response.ProductSimpleDto;
import com.product_inventory_service.product_inventory_service.entity.Product;
import com.product_inventory_service.product_inventory_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product productDetails) {
        return ResponseEntity.ok(productService.updateProduct(id, productDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user info", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/simple")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductSimpleDto>> getAllProductsSimple() {
        return ResponseEntity.ok(productService.getAllProductsSimple());
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Product> uploadProductImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file // Nhận file từ form-data
    ) {
        try {
            Product updatedProduct = productService.updateProductImage(id, file);
            return ResponseEntity.ok(updatedProduct);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null); // (Nên dùng AppExceptionHandler)
        }
    }
}