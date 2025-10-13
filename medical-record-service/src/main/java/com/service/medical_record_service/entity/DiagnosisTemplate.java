package com.service.medical_record_service.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diagnosis_templates")
@Data
public class DiagnosisTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String templateName;

    @Column(columnDefinition = "TEXT")
    private String diagnosisContent;

    @Column(name = "icd10Code")
    private String icd10Code;

    @Column(nullable = false)
    private UUID doctorId;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PrescriptionTemplateItem> prescriptionItems;
}