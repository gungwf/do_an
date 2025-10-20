package com.service.medical_record_service.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MedicalRecordRequest {
    private UUID appointmentId;
    private String diagnosis;
    private List<PrescriptionItemRequest> prescriptionItems;
    private String icd10Code;
    private UUID templateId;
}