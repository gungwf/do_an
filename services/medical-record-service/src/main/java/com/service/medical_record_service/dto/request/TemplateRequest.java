package com.service.medical_record_service.dto.request;

import com.service.medical_record_service.dto.response.PrescriptionTemplateItemDto;
import lombok.Data;

import java.util.List;

@Data
public class TemplateRequest {
    private String templateName;
    private String diagnosisContent;
    private String icd10Code;
    private List<PrescriptionTemplateItemDto> prescriptionItems;
}