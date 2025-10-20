package com.service.medical_record_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ERROR_CODE {
    UNKNOWN_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED(1002, "Bạn không có quyền thực hiện chức năng này", HttpStatus.FORBIDDEN),

    SERVICE_NOT_FOUND(5001, "Không tìm thấy dịch vụ", HttpStatus.NOT_FOUND),
    MEDICAL_RECORD_LOCKED(5002, "Bệnh án đã bị khoá", HttpStatus.BAD_REQUEST),

    // Lỗi nghiệp vụ cho Product & Inventory (7xxx)
    PRODUCT_NOT_FOUND(7001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    INVENTORY_NOT_FOUND(7002, "Sản phẩm này chưa được nhập kho tại chi nhánh", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(7003, "Không đủ số lượng tồn kho", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_TYPE_FOR_PRESCRIPTION(7004,"Không đúng loại sản phẩm",HttpStatus.BAD_REQUEST),
    DUPLICATE_MEDICAL_RECORD(7005,"Medical record for this appointment already exists.",HttpStatus.BAD_REQUEST),
    MEDICAL_RECORD_NOT_FOUND(7006,"Không có bệnh án" ,HttpStatus.NOT_FOUND ),
    BRANCH_INFO_MISSING(1003,"Không tìm thấy chi nhánh" ,HttpStatus.NOT_FOUND );

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ERROR_CODE(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}