package com.service.appointment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDoctorScheduleDto {
    private UUID id;
    private UUID doctorId;
    private Integer dayOfWeek; // 0=CN, 1=T2, ...
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isActive;
}
