package com.reporting_service.reporting_service.client;

import java.math.BigDecimal;

// Đây là DTO để hứng dữ liệu từ appointment-service
public record AppointmentResponseDto(BigDecimal priceAtBooking) {}
