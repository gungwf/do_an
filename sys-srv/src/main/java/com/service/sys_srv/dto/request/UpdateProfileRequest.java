package com.service.sys_srv.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String allergies;
    private String contraindications;
    private String medicalHistory;
}