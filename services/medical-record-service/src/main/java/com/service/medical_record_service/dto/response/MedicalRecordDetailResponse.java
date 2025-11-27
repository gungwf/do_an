package com.service.medical_record_service.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordDetailResponse {
  private UUID id;
  private UUID appointmentId;
  private String diagnosis;
  private String icd10Code;
  private boolean locked;
  private String eSignature;
  private Instant createdAt;
  private Instant updatedAt;
  private List<PerformedServiceDto> performedServices;
  private List<PrescriptionItemDto> prescriptionItems;
}
