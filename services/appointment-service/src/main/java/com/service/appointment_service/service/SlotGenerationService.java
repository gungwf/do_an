package com.service.appointment_service.service;

import com.service.appointment_service.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SlotGenerationService {

    private final AppointmentRepository appointmentRepository;

    // Hàm tạo ra các slot cố định trong 1 ngày
    private List<LocalTime> generateFixedSlots() {
        List<LocalTime> slots = new ArrayList<>();

        // Khung giờ sáng: 8:00 -> 10:15 (slot cuối là 10:15)
        LocalTime morningSlot = LocalTime.of(8, 0);
        while (morningSlot.isBefore(LocalTime.of(10, 30))) {
            slots.add(morningSlot);
            morningSlot = morningSlot.plusMinutes(15);
        }

        // Khung giờ chiều: 13:00 -> 15:15 (slot cuối là 15:15)
        LocalTime afternoonSlot = LocalTime.of(13, 0);
        while (afternoonSlot.isBefore(LocalTime.of(15, 30))) {
            slots.add(afternoonSlot);
            afternoonSlot = afternoonSlot.plusMinutes(15);
        }
        return slots;
    }

    public List<LocalTime> getAvailableSlots(UUID doctorId, LocalDate date) {
        // 1. Lấy tất cả các slot cố định trong ngày
        List<LocalTime> allFixedSlots = generateFixedSlots();

        // 2. Lấy tất cả các lịch hẹn đã bị đặt của bác sĩ trong ngày đó
        OffsetDateTime startOfDay = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        OffsetDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        Set<LocalTime> bookedSlots = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay)
                .stream()
                .map(appointment -> appointment.getAppointmentTime().toLocalTime())
                .collect(Collectors.toSet());

        // 3. Lọc và trả về các slot còn trống
        return allFixedSlots.stream()
                .filter(slot -> !bookedSlots.contains(slot))
                .collect(Collectors.toList());
    }
}