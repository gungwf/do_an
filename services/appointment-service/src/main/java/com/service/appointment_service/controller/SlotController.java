package com.service.appointment_service.controller;

import com.service.appointment_service.service.SlotGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotGenerationService slotService;

    @GetMapping("/available")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // API này không cần bảo mật vì nó chỉ hiển thị lịch trống
        return ResponseEntity.ok(slotService.getAvailableSlots(doctorId, date));
    }
}