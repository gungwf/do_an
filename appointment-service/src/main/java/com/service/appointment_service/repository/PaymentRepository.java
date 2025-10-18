package com.service.appointment_service.repository;

import com.service.appointment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(String orderId);
//    Optional<Payment> findByAppointmentId(UUID appointmentId);
//    Optional<Payment> findByTransactionId(String transactionId);
}