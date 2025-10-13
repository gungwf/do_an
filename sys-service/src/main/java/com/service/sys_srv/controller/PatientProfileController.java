package com.service.sys_srv.controller;

import com.service.sys_srv.dto.request.UpdateProfileRequest;
import com.service.sys_srv.dto.response.UserDto;
import com.service.sys_srv.entity.PatientProfile;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/patient-profiles")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PatientProfileController {

    private final AuthService authService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<PatientProfile> getPatientProfile(@PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(authService.getPatientProfileByUserId(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<PatientProfile> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        String userEmail = authentication.getName();
        UserDto userDto = authService.getUserByEmail(userEmail);
        PatientProfile updatedProfile = authService.updatePatientProfile(userDto.getId(), request);

        return ResponseEntity.ok(updatedProfile);
    }
}