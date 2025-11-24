package com.service.medical_record_service.dto.response;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformedServiceDto {
  private UUID serviceId;
  private String serviceName;
  private BigDecimal price;
}
