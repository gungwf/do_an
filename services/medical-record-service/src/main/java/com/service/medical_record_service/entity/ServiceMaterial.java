package com.service.medical_record_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "service_materials")
@Data
public class ServiceMaterial {

    @EmbeddedId
    private ServiceMaterialId id;

    @Column(nullable = false)
    private Integer quantityConsumed;
}