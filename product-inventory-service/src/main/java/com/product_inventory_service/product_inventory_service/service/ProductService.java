package com.product_inventory_service.product_inventory_service.service;

import com.product_inventory_service.product_inventory_service.dto.ProductSimpleDto;
import com.product_inventory_service.product_inventory_service.entity.Product;
import com.product_inventory_service.product_inventory_service.exception.AppException;
import com.product_inventory_service.product_inventory_service.exception.ERROR_CODE;
import com.product_inventory_service.product_inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ERROR_CODE.PRODUCT_NOT_FOUND));
    }

    public Product updateProduct(UUID id, Product productDetails) {
        Product existingProduct = getProductById(id);
        existingProduct.setProductName(productDetails.getProductName());
        existingProduct.setDescription(productDetails.getDescription());
        existingProduct.setPrice(productDetails.getPrice());
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(UUID id) {
        Product existingProduct = getProductById(id);
        existingProduct.setActive(false); // Soft delete
        productRepository.save(existingProduct);
    }

    public List<ProductSimpleDto> getAllProductsSimple() {
        return productRepository.findAll()
                .stream()
                .filter(Product::isActive) // Chỉ lấy sản phẩm đang hoạt động
                .map(product -> new ProductSimpleDto(product.getId(), product.getProductName()))
                .collect(Collectors.toList());
    }
}