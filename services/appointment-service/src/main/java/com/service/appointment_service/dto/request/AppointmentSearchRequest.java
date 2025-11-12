package com.service.appointment_service.dto.request;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AppointmentSearchRequest {
    private int page = 0;
    private int size = 10;

    private String patientName;
    private String doctorName;

    private UUID branchId;
    private UUID serviceId;

    private String status;
    private String notes;

    private Instant startTime;
    private Instant endTime;
}
