package com.service.medical_record_service.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medical_records")
@Data

public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private UUID appointmentId;

    @Column(columnDefinition = "TEXT")
    private String diagnosis; // Chẩn đoán

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PrescriptionItem> prescriptionItems;

    private String icd10Code;

    @Column(columnDefinition = "TEXT")
    private String eSignature; // Chữ ký điện tử

    private boolean isLocked = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}