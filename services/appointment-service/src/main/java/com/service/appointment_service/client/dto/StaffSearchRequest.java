package com.service.appointment_service.client.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class StaffSearchRequest {
  private String fullName;
  private String email;
  private String phoneNumber;
  private UUID branchId; // optional filter
  private String role; // expecting 'staff' or 'admin'
  private Boolean active;
  private int page = 0;
  private int size = 1; // we only need current staff
}
