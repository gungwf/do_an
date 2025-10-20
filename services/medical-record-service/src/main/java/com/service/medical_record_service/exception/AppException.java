package com.service.medical_record_service.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ERROR_CODE errorCode;

    public AppException(ERROR_CODE errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}