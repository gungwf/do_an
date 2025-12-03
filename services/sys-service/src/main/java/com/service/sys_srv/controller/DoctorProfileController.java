package com.service.sys_srv.controller;

import com.service.sys_srv.dto.request.UpdateDoctorProfileRequest;
import com.service.sys_srv.dto.response.SpecialtySimpleDto;
import com.service.sys_srv.dto.response.MyDoctorProfileResponse;
import com.service.sys_srv.dto.response.UserDto;
import com.service.sys_srv.entity.DoctorProfile;
import com.service.sys_srv.entity.PatientProfile;
import com.service.sys_srv.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/doctor-profiles")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final AuthService authService; // Bạn có thể tách logic sang service riêng nếu muốn

    // API công khai cho bệnh nhân/frontend xem hồ sơ bác sĩ
    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorProfile> getDoctorProfile(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(authService.getDoctorProfile(doctorId));
    }

    // API cho bác sĩ tự cập nhật hồ sơ của mình
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('doctor')")
    public ResponseEntity<DoctorProfile> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateDoctorProfileRequest request
    ) {
        String email = authentication.getName();
        UserDto user = authService.getUserByEmail(email);
        DoctorProfile updatedProfile = authService.updateDoctorProfile(user.getId(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/specialties")
    public ResponseEntity<List<SpecialtySimpleDto>> getSpecialties() {
        // API này nên công khai để ai cũng xem được
        return ResponseEntity.ok(authService.getUniqueSpecialties());
    }

    @GetMapping("/me")
    public ResponseEntity<MyDoctorProfileResponse> getMyProfile(Authentication authentication) {

        String userEmail = authentication.getName();
        UserDto userDto = authService.getUserByEmail(userEmail);
        DoctorProfile profile = authService.getDoctorProfileByUserId(userDto.getId());

        MyDoctorProfileResponse response = new MyDoctorProfileResponse(userDto, profile);
        return ResponseEntity.ok(response);
    }
}