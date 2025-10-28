package com.product_inventory_service.product_inventory_service.service;

import com.product_inventory_service.product_inventory_service.dto.response.ProductSimpleDto;
import com.product_inventory_service.product_inventory_service.entity.Product;
import com.product_inventory_service.product_inventory_service.exception.AppException;
import com.product_inventory_service.product_inventory_service.exception.ERROR_CODE;
import com.product_inventory_service.product_inventory_service.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ImageUploadService imageUploadService;

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
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setImageUrl(productDetails.getImageUrl());

        return productRepository.save(existingProduct);
    }

    @Transactional
    public Product updateProductImage(UUID productId, MultipartFile file) throws IOException {
        // 1. Tìm sản phẩm
        Product product = getProductById(productId); // Tái sử dụng hàm đã có

        // 2. Upload ảnh mới lên Cloudinary
        String imageUrl = imageUploadService.uploadImage(file);

        // 3. Cập nhật trường imageUrl trong DB
        product.setImageUrl(imageUrl);

        // 4. Lưu lại và trả về
        return productRepository.save(product);
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