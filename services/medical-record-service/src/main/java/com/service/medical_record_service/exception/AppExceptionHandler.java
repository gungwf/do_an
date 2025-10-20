package com.service.medical_record_service.exception;

import com.service.medical_record_service.dto.response.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    @ExceptionHandler(value = FeignException.BadRequest.class)
    ResponseEntity<ApiResponse<?>> handlingFeignBadRequest(FeignException.BadRequest exception) {
        // Lấy message lỗi gốc từ service con (ví dụ: "Không đủ số lượng tồn kho")
        String errorMessage = exception.contentUTF8();

        // Cố gắng parse JSON để lấy message và code gốc nếu có
        // Trong trường hợp đơn giản, chúng ta chỉ cần hiển thị message là đủ
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(400) // Dùng mã 400 chung
                .message(errorMessage)
                .build();

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class) // <-- THAY ĐỔI Ở ĐÂY
    public ResponseEntity<ApiResponse<?>> handlingAuthorizationDeniedException(AuthorizationDeniedException exception) { // <-- THAY ĐỔI Ở ĐÂY
        ERROR_CODE errorCode = ERROR_CODE.UNAUTHORIZED;

        log.error("AuthorizationDeniedException: {}", exception.getMessage());

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

}