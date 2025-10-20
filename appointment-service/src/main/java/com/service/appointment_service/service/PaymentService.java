package com.service.appointment_service.service;

import com.service.appointment_service.client.client.MedicalServiceClient;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.client.dto.ServiceDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
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
        String orderId = "ORD" + System.currentTimeMillis();
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setAmount(appointment.getPriceAtBooking());
        payment.setPaymentMethod("VNPAY");
        payment.setStatus("PENDING");
        payment.setOrderId(orderId);
        paymentRepository.save(payment);

        // Map này không cần sắp xếp vì hàm buildQueryStringForHash sẽ tự sắp xếp
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(appointment.getPriceAtBooking().multiply(new BigDecimal(100)).longValue()));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan lich hen ma: " + appointmentId);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnPayUtil.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // === LOGIC TẠO LINK (ĐÃ ĐÚNG) ===
        // 1. Tạo chuỗi query string ĐÃ URL-ENCODE
        String queryString = VnPayUtil.buildQueryStringForHash(vnp_Params);

        // 2. Tạo chữ ký từ chuỗi query string đó
        String secureHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString);

        return vnPayConfig.getUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    @Transactional
    public Map<String, String> handleVnPayIpn(Map<String, String> vnp_Params) {
        Map<String, String> response = new HashMap<>();
        try {
            // Sắp xếp các tham số nhận về để tạo lại hash
            Map<String, String> sortedParams = new TreeMap<>(vnp_Params);
            String vnp_SecureHash = (String) sortedParams.remove("vnp_SecureHash");

            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    hashData.append(entry.getKey());
                    hashData.append('=');
                    hashData.append(entry.getValue());
                    hashData.append('&');
                }
            }
            String hashString = hashData.substring(0, hashData.length() - 1);
            String calculatedHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashString);

            if (!calculatedHash.equals(vnp_SecureHash)) {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            String orderId = vnp_Params.get("vnp_TxnRef");
            String responseCode = vnp_Params.get("vnp_ResponseCode");

            String amountStr = vnp_Params.get("vnp_Amount");
            if (amountStr == null || amountStr.isEmpty()) {
                response.put("RspCode", "98");
                response.put("Message", "Amount parameter is missing");
                return response;
            }
            long amountFromVnPay = Long.parseLong(amountStr) / 100;

            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return response;
            }
            if (payment.getAmount().compareTo(BigDecimal.valueOf(amountFromVnPay)) != 0) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }
            if (!"PENDING".equals(payment.getStatus())) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            if ("00".equals(responseCode)) {
                payment.setStatus("PAID");
                payment.setTransactionId(vnp_Params.get("vnp_TransactionNo"));
                Appointment appointment = payment.getAppointment();
                appointment.setStatus(AppointmentStatus.CONFIRMED);
                paymentRepository.save(payment);
                appointmentRepository.save(appointment);
            } else {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
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
                Appointment appointment = payment.getAppointment();
                appointment.setStatus(AppointmentStatus.CONFIRMED);
                paymentRepository.save(payment);
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

    public AppointmentResponseDto mapToResponseDto(Appointment appointment) {
        UserDto patient = null;
        UserDto doctor = null;
        BranchDto branch = null;
        ServiceDto service = null;

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

        try {
            service = medicalServiceClient.getServiceById(appointment.getServiceId());
        } catch (Exception e) {
            throw new AppException(ERROR_CODE.SERVICE_NOT_FOUND);
        }

        PatientDto patientDto = (patient != null) ? new PatientDto(patient.id(), patient.fullName(), patient.email()) : null;
        DoctorDto doctorDto = (doctor != null) ? new DoctorDto(doctor.id(), doctor.fullName()) : null;
        com.service.appointment_service.dto.response.ServiceDto serviceDto = (service != null) ? new com.service.appointment_service.dto.response.ServiceDto(service.id(), service.serviceName()) : null;
        BranchDto branchDto = (branch != null) ? new BranchDto(branch.id(), branch.branchName(), branch.address()) : null;

        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getStatus().toString(),
                appointment.getNotes(),
                appointment.getPriceAtBooking(),
                patientDto,
                doctorDto,
                serviceDto,
                branchDto
        );
    }
}