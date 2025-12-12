package com.service.sys_srv.dto.response;

import com.service.sys_srv.entity.Enum.UserRole;
import lombok.Data;
import java.util.UUID;

@Data
public class DoctorSearchResponseDto {
    // --- CÁC TRƯỜNG GIỐNG STAFF ---
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UUID branchId;
    private String branchName; // Thêm trường branchName
    private boolean isActive;
    private UserRole role;

    // --- CÁC TRƯỜNG THÊM CỦA DOCTOR ---
    private String specialty;
    private String degree;
}