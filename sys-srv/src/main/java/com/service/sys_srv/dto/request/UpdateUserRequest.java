package com.service.sys_srv.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String phoneNumber;
    private String role;
    private UUID branchId;
    private Boolean isActive;
}