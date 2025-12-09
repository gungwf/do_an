package com.service.appointment_service.service;

import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.repository.AppointmentRepository;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
        // 1. tất cả slot cố định (ví dụ mỗi 15 phút)
        List<LocalTime> allFixedSlots = generateFixedSlots();

        // 2. dùng timezone phù hợp (ví dụ server hoặc clinic timezone)
        ZoneId zone = ZoneId.of("Asia/Bangkok"); // hoặc ZoneId.systemDefault()
        OffsetDateTime startOfDay = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime(); // exclusive

        // Lấy appointments trong [startOfDay, endOfDay) — nếu repo hỗ trợ < end, hoặc adjust accordingly
        List<Appointment> appts = appointmentRepository
            .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay.minusNanos(1)); // hoặc implement method dạng >= start && < end

        // 3. chuẩn hoá về phút (loại bỏ giây/nano) trước khi so sánh
        Set<LocalTime> bookedSlots = appts.stream()
            .map(a -> a.getAppointmentTime()
                .withOffsetSameInstant(zone.getRules().getOffset(a.getAppointmentTime().toInstant()))
                .toLocalTime()
                .truncatedTo(ChronoUnit.MINUTES))
            .collect(Collectors.toSet());

        List<LocalTime> normalizedFixed = allFixedSlots.stream()
            .map(t -> t.truncatedTo(ChronoUnit.MINUTES))
            .collect(Collectors.toList());

        // 4. lọc
        return normalizedFixed.stream()
            .filter(slot -> !bookedSlots.contains(slot))
            .collect(Collectors.toList());
    }

}