package com.product_inventory_service.product_inventory_service.exception;

import com.product_inventory_service.product_inventory_service.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class AppExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(AppException exception) {
        ERROR_CODE errorCode = exception.getErrorCode();
        log.error("AppException: {}", errorCode.getMessage());

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingRuntimeException(Exception exception) {
        log.error("Unhandled Exception: ", exception);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ERROR_CODE.UNKNOWN_EXCEPTION.getCode())
                .message(ERROR_CODE.UNKNOWN_EXCEPTION.getMessage())
                .build();

        return ResponseEntity.status(ERROR_CODE.UNKNOWN_EXCEPTION.getStatusCode()).body(apiResponse);
    }
}