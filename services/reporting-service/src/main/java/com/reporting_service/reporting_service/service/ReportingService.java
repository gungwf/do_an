package com.reporting_service.reporting_service.service;

import com.reporting_service.reporting_service.client.AppointmentResponseDto;
import com.reporting_service.reporting_service.client.AppointmentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingService {
    private final AppointmentServiceClient appointmentServiceClient;

    public Map<String, Object> generateSummaryReport() {
        // Gọi sang appointment-service để lấy dữ liệu
        List<AppointmentResponseDto> appointments = appointmentServiceClient.getAllAppointments();

        // Thực hiện tính toán
        long totalAppointments = appointments.size();
        BigDecimal totalRevenue = appointments.stream()
                .map(AppointmentResponseDto::priceAtBooking)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Trả về kết quả dưới dạng Map
        return Map.of(
                "totalAppointments", totalAppointments,
                "totalRevenue", totalRevenue
        );
    }
}