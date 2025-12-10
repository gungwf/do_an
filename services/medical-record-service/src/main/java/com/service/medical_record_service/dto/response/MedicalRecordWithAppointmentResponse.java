package com.service.medical_record_service.dto.response;

import com.service.medical_record_service.client.dto.AppointmentResponseDto;
import java.time.Instant;
import java.util.UUID;

public class MedicalRecordWithAppointmentResponse {
    public UUID medicalRecordId;
    public AppointmentResponseDto appointment;
    public String diagnosis;
    public Instant createdAt;
    public Instant updatedAt;

    public MedicalRecordWithAppointmentResponse() {}

    public MedicalRecordWithAppointmentResponse(UUID medicalRecordId, AppointmentResponseDto appointment, String diagnosis, Instant createdAt, Instant updatedAt) {
        this.medicalRecordId = medicalRecordId;
        this.appointment = appointment;
        this.diagnosis = diagnosis;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
