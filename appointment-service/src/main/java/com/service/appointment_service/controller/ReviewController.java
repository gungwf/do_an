package com.service.appointment_service.controller;

import com.service.appointment_service.client.UserDto;
import com.service.appointment_service.client.UserServiceClient;
import com.service.appointment_service.dto.ReviewRequest;
import com.service.appointment_service.entity.Review;
import com.service.appointment_service.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReviewController {
    private final ReviewService reviewService;
    private final UserServiceClient userServiceClient;

    // patient viết review
    @PostMapping
    @PreAuthorize("hasAuthority('patient')")
    public ResponseEntity<?> createReview(
            Authentication authentication,
            @RequestBody ReviewRequest request
    ) {
        try {
            String patientEmail = authentication.getName();
            UserDto patient = userServiceClient.getUserByEmail(patientEmail);

            Review newReview = reviewService.createReview(patient.id(), request);
            return ResponseEntity.ok(newReview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // xem review theo doctor
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Review>> getReviewsForDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(reviewService.getReviewsForDoctor(doctorId));
    }

    // xem review theo service
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<Review>> getReviewsForService(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(reviewService.getReviewsForService(serviceId));
    }
}