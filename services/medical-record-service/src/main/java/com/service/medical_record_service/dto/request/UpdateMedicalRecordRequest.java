package com.service.medical_record_service.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateMedicalRecordRequest {
    private String diagnosis;
    private String icd10Code;
    private UUID templateId; // Optional: load prescription items from a template

    // Bác sĩ có thể gửi lại toàn bộ danh sách dịch vụ và đơn thuốc đã được cập nhật
    private List<UUID> serviceIds;
    private List<PrescriptionItemRequest> prescriptionItems;
}