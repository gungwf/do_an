package com.service.appointment_service.client.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class StaffSearchResponseDto {
  private UUID id;
  private String fullName;
  private String email;
  private String phoneNumber;
  private UUID branchId;
  private boolean active;
  private String role;
}
