package com.product_inventory_service.product_inventory_service.dto.request;

import lombok.Data;

@Data
public class ProductSearchRequest {

    private String search;
    private String category;

    private String sort;

    // Phân trang (Pagination)
    private int page = 0;
    private int size = 10;
}