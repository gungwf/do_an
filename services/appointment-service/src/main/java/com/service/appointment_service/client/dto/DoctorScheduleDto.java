package com.service.appointment_service.client.dto;

import java.time.LocalTime;

public record DoctorScheduleDto(Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {}