package com.service.appointment_service.dto.request;

import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {
    private String status;
}