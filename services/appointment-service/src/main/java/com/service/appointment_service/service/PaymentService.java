package com.service.appointment_service.service;

import com.service.appointment_service.client.client.MedicalServiceClient;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.client.dto.MedicalRecordDto;
import com.service.appointment_service.client.dto.UserDto;
import com.service.appointment_service.config.VnPayConfig;
import com.service.appointment_service.dto.response.AppointmentResponseDto;
import com.service.appointment_service.dto.response.BranchDto;
import com.service.appointment_service.dto.response.DoctorDto;
import com.service.appointment_service.dto.response.PatientDto;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.entity.Enum.AppointmentStatus;
import com.service.appointment_service.entity.Payment;
import com.service.appointment_service.exception.AppException;
import com.service.appointment_service.exception.ERROR_CODE;
import com.service.appointment_service.repository.AppointmentRepository;
import com.service.appointment_service.repository.PaymentRepository;
import com.service.appointment_service.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final VnPayConfig vnPayConfig;
    private final EmailService emailService;
    private final MedicalServiceClient medicalServiceClient;
    private final UserServiceClient userServiceClient;

    @Transactional
    public String createVnPayPayment(UUID appointmentId, HttpServletRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + appointmentId));
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING appointments can be paid.");
        }
        String orderId = "ORD" + appointmentId;
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(appointment.getPriceAtBooking());
        payment.setPaymentMethod("VNPAY");
        payment.setStatus("PENDING");
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(appointment.getPriceAtBooking().multiply(new BigDecimal(100)).longValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan lich hen ma: " + appointmentId);
        vnp_Params.put("vnp_OrderType", "Đặt cọc");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnPayUtil.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        String queryString = VnPayUtil.buildQueryStringForHash(vnp_Params);

        // 2. Tạo chữ ký từ chuỗi query string đó
        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return vnPayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public String createVnPayPaymentMedicalRecord (UUID appointmentId, HttpServletRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + appointmentId));

        MedicalRecordDto medicalRecord = medicalServiceClient.getRecordByAppointmentId(appointmentId);
        UUID medicalRecordId = medicalRecord.id();
        // Kiểm tra tồn kho trước khi tạo payment
        var shortages = medicalServiceClient.checkStock(medicalRecordId);
        if (shortages != null && !shortages.isEmpty()) {
            throw new AppException(ERROR_CODE.INSUFFICIENT_STOCK);
        }

        // Tổng tiền cho dịch vụ (service payment)
        BigDecimal totalAmount = medicalServiceClient.getBillTotal(medicalRecordId, "SERVICE_PAYMENT").get("totalAmount");

        // Order id dạng MR-{medicalRecordId} để dễ phân biệt với các loại thanh toán khác
        String orderId = "MR-" + medicalRecordId;
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod("VNPAY");
        payment.setStatus("PENDING");
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal(100)).longValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan hoa don ma: " + medicalRecordId);
        vnp_Params.put("vnp_OrderType", "Hoá đơn");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnPayUtil.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        String queryString = VnPayUtil.buildQueryStringForHash(vnp_Params);

        // 2. Tạo chữ ký từ chuỗi query string đó
        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return vnPayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public Map<String, String> handleVnPayReturn(Map<String, String> vnp_Params) {
        Map<String, String> response = new HashMap<>();
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");

        vnp_Params.remove("vnp_SecureHash");
        if (vnp_Params.containsKey("vnp_SecureHashType")) {
            vnp_Params.remove("vnp_SecureHashType");
        }

        String queryStringToHash = VnPayUtil.buildQueryStringForHash(vnp_Params);
        String calculatedHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryStringToHash);

        if (!calculatedHash.equals(vnp_SecureHash)) {
            response.put("status", "FAILED");
            response.put("message", "Invalid Checksum");
            return response;
        }

        String responseCode = vnp_Params.get("vnp_ResponseCode");
        String orderId = vnp_Params.get("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
                if (payment != null && "PENDING".equals(payment.getStatus())) {
                payment.setStatus("PAID");
                payment.setTransactionId(vnp_Params.get("vnp_TransactionNo"));
                payment.setPaidAt(java.time.Instant.now());
                Appointment appointment = payment.getAppointment();
                appointment.setStatus(AppointmentStatus.CONFIRMED);

                // Create a Bill record in medical-record-service for this successful payment
                try {
                    com.service.appointment_service.client.dto.BillRequestDto billReq = new com.service.appointment_service.client.dto.BillRequestDto(
                            appointment.getPatientId(),
                            null,
                            appointment.getBranchId(),
                            "APPOINTMENT_PAYMENT",
                            payment.getAmount(),
                            java.util.Collections.emptyList(),
                            "Deposit/payment for appointment " + appointment.getId()
                    );
                    java.util.Map<String, Object> billResp = medicalServiceClient.createBill(billReq);
                    if (billResp != null && billResp.containsKey("billId")) {
                        try {
                            java.util.UUID bid = java.util.UUID.fromString(billResp.get("billId").toString());
                            payment.setBillId(bid);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ex) {
                    log.error("Failed to create bill after payment: {}", ex.getMessage());
                }

                paymentRepository.save(payment);
                // If we have a linked bill, mark it as paid in medical-record-service
                try {
                    if (payment.getBillId() != null) {
                        medicalServiceClient.markBillPaid(payment.getBillId());
                    }
                } catch (Exception ex) {
                    log.error("Failed to mark bill paid after payment: {}", ex.getMessage());
                }
                appointmentRepository.save(appointment);
                AppointmentResponseDto dto = mapToResponseDto(appointment);
                emailService.sendAppointmentConfirmation(dto);
                response.put("status", "SUCCESS");
                response.put("message", "Payment successful!");
            } else if (payment != null && !"PENDING".equals(payment.getStatus())) {
                response.put("status", "ALREADY_CONFIRMED");
                response.put("message", "This order has already been confirmed.");
            } else {
                response.put("status", "FAILED");
                response.put("message", "Order not found.");
            }
        } else {
            response.put("status", "FAILED");
            response.put("message", "Payment failed or cancelled.");
        }
        return response;
    }

    @Transactional
    public String createVnPayPaymentPrescription(UUID appointmentId, HttpServletRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + appointmentId));

        MedicalRecordDto medicalRecord = medicalServiceClient.getRecordByAppointmentId(appointmentId);
        UUID medicalRecordId = medicalRecord.id();
        // Kiểm tra tồn kho trước khi tạo payment cho thuốc
        var shortages = medicalServiceClient.checkStock(medicalRecordId);
        if (shortages != null && !shortages.isEmpty()) {
            throw new AppException(ERROR_CODE.INSUFFICIENT_STOCK);
        }

        // Tổng tiền cho thuốc (drug payment)
        BigDecimal totalAmount = medicalServiceClient.getBillTotal(medicalRecordId, "DRUG_PAYMENT").get("totalAmount");

        // Order dạng PRESC-{medicalRecordId}
        String orderId = "PRESC-" + medicalRecordId;
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod("VNPAY");
        payment.setStatus("PENDING");
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal(100)).longValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan thuoc cho medicalRecord: " + medicalRecordId);
        vnp_Params.put("vnp_OrderType", "Thuoc");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnPayUtil.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        String queryString = VnPayUtil.buildQueryStringForHash(vnp_Params);
        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return vnPayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public String createVnPayPaymentForBill(UUID billId, HttpServletRequest request) {
        // Fetch bill details from medical-record-service
        Map<String, Object> bill = medicalServiceClient.getBillById(billId);
        if (bill == null || !bill.containsKey("totalAmount")) {
            throw new RuntimeException("Bill not found: " + billId);
        }

        BigDecimal totalAmount;
        try {
            Object v = bill.get("totalAmount");
            if (v instanceof BigDecimal) totalAmount = (BigDecimal) v;
            else totalAmount = new BigDecimal(v.toString());
        } catch (Exception ex) {
            throw new RuntimeException("Invalid bill totalAmount: " + ex.getMessage());
        }

        String orderId = "BILL-" + billId;
        Payment payment = new Payment();
        payment.setAmount(totalAmount);
        payment.setPaymentMethod("VNPAY");
        payment.setStatus("PENDING");
        payment.setOrderId(orderId);
        payment.setBillId(billId);
        paymentRepository.save(payment);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(totalAmount.multiply(new BigDecimal(100)).longValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan bill: " + billId);
        vnp_Params.put("vnp_OrderType", "Thuoc");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnPayUtil.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        String queryString = VnPayUtil.buildQueryStringForHash(vnp_Params);
        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return vnPayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public Map<String, String> handleVnPayReturnPrescription(Map<String, String> vnp_Params) {
        Map<String, String> response = new HashMap<>();
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");

        vnp_Params.remove("vnp_SecureHash");
        if (vnp_Params.containsKey("vnp_SecureHashType")) {
            vnp_Params.remove("vnp_SecureHashType");
        }

        String queryStringToHash = VnPayUtil.buildQueryStringForHash(vnp_Params);
        String calculatedHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryStringToHash);

        if (!calculatedHash.equals(vnp_SecureHash)) {
            response.put("status", "FAILED");
            response.put("message", "Invalid Checksum");
            return response;
        }

        String responseCode = vnp_Params.get("vnp_ResponseCode");
        String orderId = vnp_Params.get("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
                if (payment != null && "PENDING".equals(payment.getStatus())) {
                payment.setStatus("PAID");
                payment.setTransactionId(vnp_Params.get("vnp_TransactionNo"));
                payment.setPaidAt(java.time.Instant.now());
                Appointment appointment = payment.getAppointment();
                try {
                    // Nếu orderId là PRESC-{id}
                    if (orderId != null && orderId.startsWith("PRESC-")) {
                        String idPart = orderId.substring(6);
                        medicalServiceClient.triggerDeductStock(UUID.fromString(idPart));
                    } else {
                        MedicalRecordDto medicalRecord = medicalServiceClient.getRecordByAppointmentId(appointment.getId());
                        medicalServiceClient.triggerDeductStock(medicalRecord.id());
                    }
                } catch (Exception ex) {
                    log.error("Failed to trigger deduct stock for prescription: {}", ex.getMessage());
                }
                // create Bill record for prescription payment
                try {
                    com.service.appointment_service.client.dto.BillRequestDto billReq = new com.service.appointment_service.client.dto.BillRequestDto(
                            appointment.getPatientId(),
                            null,
                            appointment.getBranchId(),
                            "DRUG_PAYMENT",
                            payment.getAmount(),
                            java.util.Collections.emptyList(),
                            "Prescription payment for appointment " + appointment.getId()
                    );
                    java.util.Map<String, Object> billResp = medicalServiceClient.createBillProducts(billReq);
                    if (billResp != null && billResp.containsKey("billId")) {
                        try {
                            java.util.UUID bid = java.util.UUID.fromString(billResp.get("billId").toString());
                            payment.setBillId(bid);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ex) {
                    log.error("Failed to create bill after prescription payment: {}", ex.getMessage());
                }
                paymentRepository.save(payment);
                try {
                    if (payment.getBillId() != null) {
                        medicalServiceClient.markBillPaid(payment.getBillId());
                    }
                } catch (Exception ex) {
                    log.error("Failed to mark bill paid after prescription payment: {}", ex.getMessage());
                }
                response.put("status", "SUCCESS");
                response.put("message", "Prescription payment successful!");
            } else if (payment != null && !"PENDING".equals(payment.getStatus())) {
                response.put("status", "ALREADY_CONFIRMED");
                response.put("message", "This order has already been confirmed.");
            } else {
                response.put("status", "FAILED");
                response.put("message", "Order not found.");
            }
        } else {
            response.put("status", "FAILED");
            response.put("message", "Payment failed or cancelled.");
        }
        return response;
    }

    @Transactional
    public Map<String, String> handleVnPayReturnMedicalRecord(Map<String, String> vnp_Params) {
        Map<String, String> response = new HashMap<>();
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");

        vnp_Params.remove("vnp_SecureHash");
        if (vnp_Params.containsKey("vnp_SecureHashType")) {
            vnp_Params.remove("vnp_SecureHashType");
        }

        String queryStringToHash = VnPayUtil.buildQueryStringForHash(vnp_Params);
        String calculatedHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryStringToHash);

        if (!calculatedHash.equals(vnp_SecureHash)) {
            response.put("status", "FAILED");
            response.put("message", "Invalid Checksum");
            return response;
        }

        String responseCode = vnp_Params.get("vnp_ResponseCode");
        String orderId = vnp_Params.get("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
                if (payment != null && "PENDING".equals(payment.getStatus())) {
                payment.setStatus("PAID");
                payment.setTransactionId(vnp_Params.get("vnp_TransactionNo"));
                payment.setPaidAt(java.time.Instant.now());
                Appointment appointment = payment.getAppointment();

                // Cập nhật trạng thái appointment (xác nhận)
                appointment.setStatus(AppointmentStatus.CONFIRMED);

                // Gọi service y tế để trừ kho cho các vật tư liên quan tới dịch vụ trong medical record
                try {
                    MedicalRecordDto medicalRecord = medicalServiceClient.getRecordByAppointmentId(appointment.getId());
                    UUID medicalRecordId = medicalRecord.id();
                    // Nếu orderId là dạng MR-{id} thì lấy id từ chuỗi, ngược lại cố gắng parse
                    if (orderId != null && orderId.startsWith("MR-")) {
                        String idPart = orderId.substring(3);
                        medicalServiceClient.triggerDeductStock(UUID.fromString(idPart));
                    } else {
                        // fallback
                        medicalServiceClient.triggerDeductStock(medicalRecordId);
                    }
                } catch (Exception ex) {
                    log.error("Failed to trigger deduct stock for medical record: {}", ex.getMessage());
                }

                // create Bill record for medical-record service payment
                try {
                    com.service.appointment_service.client.dto.BillRequestDto billReq = new com.service.appointment_service.client.dto.BillRequestDto(
                            appointment.getPatientId(),
                            null,
                            appointment.getBranchId(),
                            "SERVICE_PAYMENT",
                            payment.getAmount(),
                            java.util.Collections.emptyList(),
                            "Service payment for appointment " + appointment.getId()
                    );
                    java.util.Map<String, Object> billResp = medicalServiceClient.createBill(billReq);
                    if (billResp != null && billResp.containsKey("billId")) {
                        try {
                            java.util.UUID bid = java.util.UUID.fromString(billResp.get("billId").toString());
                            payment.setBillId(bid);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ex) {
                    log.error("Failed to create bill after medical record payment: {}", ex.getMessage());
                }

                paymentRepository.save(payment);
                try {
                    if (payment.getBillId() != null) {
                        medicalServiceClient.markBillPaid(payment.getBillId());
                    }
                } catch (Exception ex) {
                    log.error("Failed to mark bill paid after medical record payment: {}", ex.getMessage());
                }
                appointmentRepository.save(appointment);
                try {
                    AppointmentResponseDto dto = mapToResponseDto(appointment);
                    emailService.sendAppointmentConfirmation(dto);
                } catch (Exception ignored) {}

                response.put("status", "SUCCESS");
                response.put("message", "Payment successful!");
            } else if (payment != null && !"PENDING".equals(payment.getStatus())) {
                response.put("status", "ALREADY_CONFIRMED");
                response.put("message", "This order has already been confirmed.");
            } else {
                response.put("status", "FAILED");
                response.put("message", "Order not found.");
            }
        } else {
            response.put("status", "FAILED");
            response.put("message", "Payment failed or cancelled.");
        }
        return response;
    }

    public AppointmentResponseDto mapToResponseDto(Appointment appointment) {
        UserDto patient = null;
        UserDto doctor = null;
        BranchDto branch = null;

        try {
            patient = userServiceClient.getUserById(appointment.getPatientId());
        } catch (Exception e) {
            throw new AppException(ERROR_CODE.PATIENT_NOT_FOUND);
        }

        if (appointment.getDoctorId() != null) {
            try {
                doctor = userServiceClient.getUserById(appointment.getDoctorId());
            } catch (Exception e) {
                throw new AppException(ERROR_CODE.DOCTOR_NOT_FOUND);
            }
        }

        try {
            branch = userServiceClient.getBranchById(appointment.getBranchId());
        } catch (Exception e) {
            throw new AppException(ERROR_CODE.BRANCH_NOT_FOUND);
        }

        PatientDto patientDto = (patient != null) ? new PatientDto(patient.id(), patient.fullName(), patient.email()) : null;
        DoctorDto doctorDto = (doctor != null) ? new DoctorDto(doctor.id(), doctor.fullName()) : null;
        BranchDto branchDto = (branch != null) ? new BranchDto(branch.id(), branch.branchName(), branch.address()) : null;

        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getAppointmentTime(),
                appointment.getStatus().toString(),
                appointment.getNotes(),
                appointment.getPriceAtBooking(),
                patientDto,
                doctorDto,
                branchDto
        );
    }
}