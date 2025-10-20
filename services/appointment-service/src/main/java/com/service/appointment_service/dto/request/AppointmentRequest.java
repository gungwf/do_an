package com.service.appointment_service.dto.request;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AppointmentRequest {
    private UUID doctorId;
    private UUID serviceId;
    private UUID branchId;
    private OffsetDateTime appointmentTime;
    private String notes;
}