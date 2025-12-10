package com.service.medical_record_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public class MedicalRecordSummaryResponse {
    public UUID id;
    public UUID appointmentId;
    public String diagnosis;
    public Instant createdAt;
    public Instant updatedAt;

    public MedicalRecordSummaryResponse() {}

    public MedicalRecordSummaryResponse(UUID id, UUID appointmentId, String diagnosis, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.diagnosis = diagnosis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
