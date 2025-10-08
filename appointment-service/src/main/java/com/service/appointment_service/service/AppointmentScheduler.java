package com.service.appointment_service.service;

import com.service.appointment_service.dto.AppointmentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentScheduler {

    private final AppointmentService appointmentService;
    private final EmailService emailService;

    @Scheduled(fixedRate = 3600000)
    public void sendAppointmentReminders() {
        log.info("--- Bắt đầu quét và gửi email nhắc lịch ---");

        // 1. Tìm các lịch hẹn cần nhắc
        List<AppointmentResponseDto> appointmentsToRemind = appointmentService.findAppointmentsForReminder();

        if (appointmentsToRemind.isEmpty()) {
            log.info("--- Không có lịch hẹn nào cần nhắc trong 1 giờ tới ---");
            return;
        }

        // 2. Lặp qua và gửi email cho từng lịch hẹn
        for (AppointmentResponseDto appointment : appointmentsToRemind) {
            try {
                emailService.sendAppointmentReminder(appointment);
                log.info("Đã gửi email nhắc lịch cho cuộc hẹn ID: {}", appointment.id());
                // (Thêm logic để đánh dấu là đã gửi email nhắc để không gửi lại)
            } catch (Exception e) {
                log.error("Lỗi khi gửi email nhắc lịch cho cuộc hẹn ID: {}", appointment.id(), e);
            }
        }

        log.info("--- Hoàn thành việc gửi email nhắc lịch. Tổng cộng: {} email. ---", appointmentsToRemind.size());
    }
}