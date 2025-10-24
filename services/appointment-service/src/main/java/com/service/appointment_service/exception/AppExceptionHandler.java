package com.service.appointment_service.exception;

import com.service.appointment_service.dto.response.ApiResponse;
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
        String errorMessage = exception.contentUTF8();
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(400)
                .message(errorMessage)
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
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