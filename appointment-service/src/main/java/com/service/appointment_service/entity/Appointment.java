package com.service.appointment_service.entity;

import com.service.appointment_service.entity.Enum.AppointmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Data
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    private UUID doctorId;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private OffsetDateTime appointmentTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private String notes;

    @Column(nullable = false)
    private BigDecimal priceAtBooking;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
