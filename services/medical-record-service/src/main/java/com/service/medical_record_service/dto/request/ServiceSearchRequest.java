package com.service.medical_record_service.dto.request;

import lombok.Data;

@Data
public class ServiceSearchRequest {
    private int page = 0;
    private int size = 10;
    private String search;
    private String sortBy = "id";
    private String sortDir = "asc";
}