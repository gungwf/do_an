package com.service.sys_srv.dto.request;

import lombok.Data;

@Data
public class UpdateDoctorProfileRequest {
    private String specialty;
    private String degree;

}