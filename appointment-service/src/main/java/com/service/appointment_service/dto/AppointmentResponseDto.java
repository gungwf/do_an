package com.service.appointment_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponseDto(
        UUID id,
        OffsetDateTime appointmentTime,
        Integer durationMinutes,
        String status,
        String notes,
        BigDecimal priceAtBooking,
        PatientDto patient,
        DoctorDto doctor,
        ServiceDto service,
        BranchDto branch
) {}

