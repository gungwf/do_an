package com.service.medical_record_service.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "protocol_services")
@Data
public class ProtocolServiceLink {
    @EmbeddedId
    private ProtocolServiceId id;
}