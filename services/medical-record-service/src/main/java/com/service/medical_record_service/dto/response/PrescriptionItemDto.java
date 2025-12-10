package com.service.medical_record_service.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemDto {
  private UUID id;
  private UUID productId;
  private Integer quantity;
  private String dosage;
  private String notes;
  private String productName;
}
