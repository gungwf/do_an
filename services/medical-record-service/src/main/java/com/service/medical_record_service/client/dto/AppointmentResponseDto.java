package com.service.medical_record_service.client.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponseDto(
	UUID id,
	OffsetDateTime appointmentTime,
	String status,
	String notes,
	BigDecimal priceAtBooking,
	PatientDto patient,
	DoctorDto doctor,
	BranchDto branch
) {}

