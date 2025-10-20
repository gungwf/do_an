package com.service.appointment_service.controller;

import com.service.appointment_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleVnPayIpn(
            @RequestParam Map<String, String> allParams) {
        Map<String, String> response = paymentService.handleVnPayIpn(allParams);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<Map<String, String>> handleVnPayReturn(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.handleVnPayReturn(allParams);
        return ResponseEntity.ok(result);
    }
}