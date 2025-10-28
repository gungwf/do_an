package com.service.sys_srv.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class DoctorSearchRequest {
    private String name;
    private String specialty;
    private UUID branchId;

    private int page = 0;
    private int size = 10;
}