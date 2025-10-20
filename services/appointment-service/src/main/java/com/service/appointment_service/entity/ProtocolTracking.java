package com.service.appointment_service.entity;

import com.service.appointment_service.entity.Enum.ProtocolStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "protocol_tracking")
@Data
public class ProtocolTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "protocol_service_id", nullable = false)
    private UUID protocolServiceId;

    @Column(nullable = false)
    private Integer totalSessions;

    @Column
    private Integer completedSessions = 0;

    @Enumerated(EnumType.STRING)
    @Column
    private ProtocolStatus status = ProtocolStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "start_date")
    private Instant startDate;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;
}