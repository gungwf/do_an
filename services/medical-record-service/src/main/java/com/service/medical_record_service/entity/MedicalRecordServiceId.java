package com.service.medical_record_service.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class MedicalRecordServiceId implements Serializable {
    private UUID medicalRecordId;
    private UUID serviceId;
}