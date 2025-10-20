package com.service.appointment_service.service;

import com.service.appointment_service.dto.request.ReviewRequest;
import com.service.appointment_service.entity.Appointment;
import com.service.appointment_service.entity.Enum.AppointmentStatus;
import com.service.appointment_service.entity.Review;
import com.service.appointment_service.repository.AppointmentRepository;
import com.service.appointment_service.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    public Review createReview(UUID patientId, ReviewRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!appointment.getPatientId().equals(patientId)) {
            throw new AccessDeniedException("You are not authorized to review this appointment.");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot review an appointment that is not completed.");
        }

        //  có thể thêm logic kiểm tra xem lịch hẹn này đã được review chưa
        Review review = new Review();
        review.setAppointmentId(request.getAppointmentId());
        review.setPatientId(patientId);
        review.setServiceId(appointment.getServiceId());
        review.setDoctorId(appointment.getDoctorId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsForDoctor(UUID doctorId) {
        return reviewRepository.findByDoctorId(doctorId);
    }

    public List<Review> getReviewsForService(UUID serviceId) {
        return reviewRepository.findByServiceId(serviceId);
    }
}