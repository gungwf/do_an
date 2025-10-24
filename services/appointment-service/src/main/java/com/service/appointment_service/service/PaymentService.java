package com.service.appointment_service.service;

import com.service.appointment_service.client.client.MedicalServiceClient;
import com.service.appointment_service.client.client.UserServiceClient;
import com.service.appointment_service.client.dto.MedicalRecordDto;
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
        BigDecimal totalAmount = medicalServiceClient.getBillTotal(medicalRecordId).get("totalAmount");

        String orderId = "" + medicalRecordId;
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
                Appointment appointment = payment.getAppointment();
                MedicalRecordDto medicalRecord = medicalServiceClient.getRecordByAppointmentId(appointment.getId());
                UUID medicalRecordId = medicalRecord.id();
                medicalServiceClient.triggerDeductStock(UUID.fromString(orderId));
                paymentRepository.save(payment);
//                AppointmentResponseDto dto = mapToResponseDto(appointment);
//                emailService.sendAppointmentConfirmation(dto);
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