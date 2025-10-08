package com.service.medical_record_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Bỏ qua các trường null khi chuyển sang JSON
public class ApiResponse<T> {
    private int code;
    private String message;
    private T result;
}