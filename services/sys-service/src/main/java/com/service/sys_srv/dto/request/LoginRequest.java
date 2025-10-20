package com.service.sys_srv.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}