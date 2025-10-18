package com.service.appointment_service.service;

import com.service.appointment_service.config.VnPayConfig;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.entity.Enum.AppointmentStatus;
import com.service.appointment_service.entity.Payment;
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

        // Lấy vnp_SecureHash từ tham số
        String vnp_SecureHash = vnp_Params.get("vnp_SecureHash");

        // Xóa vnp_SecureHash và vnp_SecureHashType (nếu có)
        vnp_Params.remove("vnp_SecureHash");
        if (vnp_Params.containsKey("vnp_SecureHashType")) {
            vnp_Params.remove("vnp_SecureHashType");
        }

        // === LOGIC KIỂM TRA HASH ĐÃ SỬA LẠI ===
        // 1. Dùng chung hàm để tạo lại chuỗi query string ĐÃ URL-ENCODE
        String queryStringToHash = VnPayUtil.buildQueryStringForHash(vnp_Params);

        // 2. Tạo lại chữ ký từ chuỗi đó
        String calculatedHash = VnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryStringToHash);

        // So sánh 2 chữ ký
        if (!calculatedHash.equals(vnp_SecureHash)) {
            response.put("status", "FAILED");
            response.put("message", "Invalid Checksum");
            return response; // Trả về lỗi ngay lập tức nếu chữ ký không khớp
        }

        // === LOGIC NGHIỆP VỤ (Giữ nguyên) ===
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
}