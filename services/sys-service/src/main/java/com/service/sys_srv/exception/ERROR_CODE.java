package com.service.sys_srv.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ERROR_CODE {
    UNKNOWN_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),

    // Lỗi nghiệp vụ cho Product & Inventory (4xxx)
    PRODUCT_NOT_FOUND(4001, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    INVENTORY_NOT_FOUND(4002, "Sản phẩm này chưa được nhập kho tại chi nhánh", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(4003, "Không đủ số lượng tồn kho", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_TYPE_FOR_PRESCRIPTION(4004,"Không đúng loại sản phẩm",HttpStatus.BAD_REQUEST),
    DUPLICATE_MEDICAL_RECORD(4005,"Medical record for this appointment already exists.",HttpStatus.BAD_REQUEST),

    ACCESS_DENY(1001,"Bạn không có quyền", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(1002,"Không tìm thấy tài khoản",HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL(1005,"Email đã tồn tại!",HttpStatus.CONFLICT),
    PATIENT_PROFILE_NOT_FOUND(1003,"Không tìm hồ sơ bệnh nhân",HttpStatus.NOT_FOUND),
    ILLEGAL_ROLE(1004,"Vai trò không phù hợp",HttpStatus.BAD_REQUEST),
    BRANCH_NOT_FOUND(1006,"Không tìm thấy Cơ sở",HttpStatus.NOT_FOUND);
    PROFILE_NOT_FOUND(1007,"Không tìm thấy hồ sơ",HttpStatus.NOT_FOUND);
    
    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ERROR_CODE(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}