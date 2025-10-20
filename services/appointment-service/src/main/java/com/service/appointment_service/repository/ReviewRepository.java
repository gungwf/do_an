package com.service.appointment_service.repository;

import com.service.appointment_service.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByDoctorId(UUID doctorId);
    List<Review> findByServiceId(UUID serviceId);
}