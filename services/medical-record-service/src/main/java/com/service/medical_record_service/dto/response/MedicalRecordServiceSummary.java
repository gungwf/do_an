package com.service.medical_record_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordServiceSummary {
  private UUID id;
  private String serviceName;
  private BigDecimal price;
}
