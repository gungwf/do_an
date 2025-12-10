package com.service.medical_record_service.client.dto;

/**
 * Generic wrapper matching product-inventory-service ApiResponse<T>
 */
public record ApiResponse<T>(Integer code, String message, T result) {}
