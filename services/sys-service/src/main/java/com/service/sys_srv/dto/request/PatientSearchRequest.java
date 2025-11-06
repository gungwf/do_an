package com.service.sys_srv.dto.request;

import lombok.Data;

@Data
public class PatientSearchRequest {

    private String fullName;
    private String email;
    private String phoneNumber;
    private String membershipTier;
    private Boolean active;

    private int page = 0;
    private int size = 10;
}