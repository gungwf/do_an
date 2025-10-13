package com.service.sys_srv.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String phoneNumber;
    private UUID branchId;
    private String role;
}