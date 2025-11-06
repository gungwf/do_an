package com.product_inventory_service.product_inventory_service.service;

import com.product_inventory_service.product_inventory_service.dto.request.ProductSearchRequest;
import com.product_inventory_service.product_inventory_service.dto.response.CategorySimpleDto;
import com.product_inventory_service.product_inventory_service.dto.response.ProductSearchResponseDto;
import com.product_inventory_service.product_inventory_service.dto.response.ProductSimpleDto;
import com.product_inventory_service.product_inventory_service.entity.Product;
import com.product_inventory_service.product_inventory_service.exception.AppException;
import com.product_inventory_service.product_inventory_service.exception.ERROR_CODE;
import com.product_inventory_service.product_inventory_service.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    public Page<ProductSearchResponseDto> searchProducts(ProductSearchRequest request) {

        // --- 1. XỬ LÝ SẮP XẾP (SORTING) ---
        Sort sort;
        if ("price_asc".equals(request.getSort())) {
            sort = Sort.by("price").ascending();
        } else if ("price_desc".equals(request.getSort())) {
            sort = Sort.by("price").descending();
        } else {
            sort = Sort.by("productName").ascending(); // Mặc định sắp xếp theo tên
        }

        // --- 2. TẠO ĐỐI TƯỢNG PHÂN TRANG (PAGEABLE) ---
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // --- 3. TẠO ĐỐI TƯỢNG LỌC (SPECIFICATION) ---
        // Bắt đầu với một Specification "trống" (luôn đúng)
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        // Lọc theo tên (search) nếu có
        if (StringUtils.hasText(request.getSearch())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("productName")), "%" + request.getSearch().toLowerCase() + "%")
            );
        }

        // Lọc theo danh mục (category) nếu có
        if (StringUtils.hasText(request.getCategory())) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category"), request.getCategory())
            );
        }

        // --- 4. GỌI REPOSITORY ---
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // --- 5. CHUYỂN ĐỔI (MAP) SANG DTO ĐỂ TRẢ VỀ ---
        return productPage.map(this::convertToProductSearchResponseDto);
    }

    /**
     * Hàm helper để chuyển đổi Product sang DTO Response
     */
    private ProductSearchResponseDto convertToProductSearchResponseDto(Product product) {
        ProductSearchResponseDto dto = new ProductSearchResponseDto();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setProductType(product.getProductType());
        dto.setCategory(product.getCategory());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.isActive());
        return dto;
    }

    public List<CategorySimpleDto> getUniqueCategories() {
        List<String> categoryNames = productRepository.findDistinctCategories();

        return categoryNames.stream()
                .map(name -> new CategorySimpleDto(name, name))
                .collect(Collectors.toList());
    }
}