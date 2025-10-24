package com.service.appointment_service.controller;

import com.service.appointment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-payment/{appointmentId}")
    public ResponseEntity<String> createPayment(
            @PathVariable UUID appointmentId,
            HttpServletRequest request) {
        String paymentUrl = paymentService.createVnPayPayment(appointmentId, request);
        return ResponseEntity.ok(paymentUrl);
    }


    @GetMapping("/vnpay-return")
    public ResponseEntity<Map<String, String>> handleVnPayReturn(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.handleVnPayReturn(allParams);
        return ResponseEntity.ok(result);
    }

    //Thanh toán hoá đơn
    @PostMapping("/{id}/generate-bill-payment")
    @PreAuthorize("hasAnyAuthority('admin', 'staff')")
    public ResponseEntity<?> generateBillPayment(
            @PathVariable UUID id,
            HttpServletRequest httpServletRequest
    ) {
        String payUrl = paymentService.createVnPayPaymentMedicalRecord(id, httpServletRequest);
        return ResponseEntity.ok(Map.of("payUrl", payUrl));
    }

    @GetMapping("/vnpay-return-medical-record")
    public ResponseEntity<Map<String, String>> handleVnPayReturnMedicalRecord(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.handleVnPayReturnMedicalRecord(allParams);
        return ResponseEntity.ok(result);
    }
}