package com.service.medical_record_service.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "medical_record_services")
@Data
public class MedicalRecordServiceLink {

    @EmbeddedId
    private MedicalRecordServiceId id;

    @ManyToOne
    @MapsId("medicalRecordId") // Ánh xạ phần medicalRecordId của @EmbeddedId
    @JoinColumn(name = "medical_record_id")
    @JsonBackReference // Tránh lặp vô tận khi chuyển JSON
    private MedicalRecord medicalRecord;

}