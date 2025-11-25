package com.service.appointment_service.controller;

import com.service.appointment_service.service.PaymentService;
import com.service.appointment_service.client.client.MedicalServiceClient;
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
    private final MedicalServiceClient medicalServiceClient;

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
        // Kiểm tra tồn kho trước
        var medicalRecord = medicalServiceClient.getRecordByAppointmentId(id);
        var shortages = medicalServiceClient.checkStock(medicalRecord.id());
        if (shortages != null && !shortages.isEmpty()) {
            return ResponseEntity.status(409).body(shortages);
        }
        String payUrl = paymentService.createVnPayPaymentMedicalRecord(id, httpServletRequest);
        return ResponseEntity.ok(Map.of("payUrl", payUrl));
    }

    @GetMapping("/vnpay-return-medical-record")
    public ResponseEntity<Map<String, String>> handleVnPayReturnMedicalRecord(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.handleVnPayReturnMedicalRecord(allParams);
        return ResponseEntity.ok(result);
    }

    // Thanh toán đơn thuốc (prescription)
    @PostMapping("/{id}/generate-prescription-payment")
    public ResponseEntity<?> generatePrescriptionPayment(
            @PathVariable UUID id,
            HttpServletRequest httpServletRequest
    ) {
        // This endpoint is legacy (accepts appointmentId). Prefer using bill-based flow.
        var medicalRecord2 = medicalServiceClient.getRecordByAppointmentId(id);
        var shortages2 = medicalServiceClient.checkStock(medicalRecord2.id());
        if (shortages2 != null && !shortages2.isEmpty()) {
            return ResponseEntity.status(409).body(shortages2);
        }
        String payUrl = paymentService.createVnPayPaymentPrescription(id, httpServletRequest);
        return ResponseEntity.ok(Map.of("payUrl", payUrl));
    }

    // New: generate payment for a Bill (used by purchase flow)
    @PostMapping("/bills/{billId}/generate-payment")
    public ResponseEntity<?> generatePaymentForBill(@PathVariable UUID billId, HttpServletRequest request) {
        // Check stock for bill
//        var shortages = medicalServiceClient.checkStockForBill(billId);
//        if (shortages != null && !shortages.isEmpty()) {
//            return ResponseEntity.status(409).body(shortages);
//        }
        String payUrl = paymentService.createVnPayPaymentForBill(billId, request);
        return ResponseEntity.ok(Map.of("payUrl", payUrl));
    }

    @GetMapping("/vnpay-return-prescription")
    public ResponseEntity<Map<String, String>> handleVnPayReturnPrescription(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.handleVnPayReturnPrescription(allParams);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/vnpay-return-prescription-online")
    public ResponseEntity<Map<String, String>> handleVnPayReturnPrescriptionOnline(@RequestParam Map<String, String> allParams) {
        Map<String, String> result = paymentService.handleVnPayReturnPrescription(allParams);
        return ResponseEntity.ok(result);
    }
}