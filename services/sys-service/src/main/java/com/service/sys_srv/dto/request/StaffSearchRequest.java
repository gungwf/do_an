package com.service.sys_srv.dto.request;
import com.service.sys_srv.entity.Enum.UserRole;
import lombok.Data;
import java.util.UUID;

@Data
public class StaffSearchRequest {

    // 1. Tiêu chí lọc (Filtering)
    private String fullName; // Lọc theo fullName
    private String email;
    private String phoneNumber;
    private UUID branchId;
    private UserRole role; // Cho phép lọc theo vai trò cụ thể (staff hoặc admin)
    private Boolean active;

    // 2. Phân trang (Pagination)
    private int page = 0;
    private int size = 10;
}