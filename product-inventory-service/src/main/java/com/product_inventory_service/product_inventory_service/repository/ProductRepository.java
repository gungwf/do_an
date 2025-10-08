package com.product_inventory_service.product_inventory_service.repository;

import com.product_inventory_service.product_inventory_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {}