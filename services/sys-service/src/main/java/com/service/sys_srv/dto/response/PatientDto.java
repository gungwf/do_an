package com.service.sys_srv.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class PatientDto {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private boolean isActive;
    // Thông tin thêm từ profile
    private String membershipTier;
    private Integer points;
}