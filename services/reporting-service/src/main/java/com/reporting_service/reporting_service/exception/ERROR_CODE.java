package com.reporting_service.reporting_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ERROR_CODE {
    UNKNOWN_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Lỗi nghiệp vụ cho Product & Inventory (7xxx)
    PRODUCT_NOT_FOUND(7001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    INVENTORY_NOT_FOUND(7002, "Sản phẩm này chưa được nhập kho tại chi nhánh", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(7003, "Không đủ số lượng tồn kho", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ERROR_CODE(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}