package com.service.sys_srv.dto.response;

import com.service.sys_srv.entity.Enum.UserRole;
import lombok.Data;
import java.util.UUID;

@Data
public class StaffSearchResponseDto {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UUID branchId;
    private boolean isActive;
    private UserRole role;
}