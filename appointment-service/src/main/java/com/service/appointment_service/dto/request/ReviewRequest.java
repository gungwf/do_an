package com.service.appointment_service.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequest {
    private UUID appointmentId;
    private Integer rating;
    private String comment;
}