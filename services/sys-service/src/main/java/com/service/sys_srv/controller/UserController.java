package com.service.sys_srv.controller;

import com.service.sys_srv.dto.request.DoctorSearchRequest;
import com.service.sys_srv.dto.request.UpdateUserRequest;
import com.service.sys_srv.dto.response.DoctorSearchResponseDto;
import com.service.sys_srv.dto.response.StaffDto;
import com.service.sys_srv.dto.response.UserDto;
import com.service.sys_srv.dto.response.UserSimpleDto;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {
    private final AuthService authService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        try {
            UserDto userDto = authService.getUserById(id);
            return ResponseEntity.ok(userDto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUser() {
        return ResponseEntity.ok(authService.getAllUser());
    }

    // Chỉ admin hoặc staff mới được xem danh sách bác sĩ
    @GetMapping("/doctors")
    public ResponseEntity<List<StaffDto>> getDoctors() {
        return ResponseEntity.ok(authService.getDoctors());
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserDto> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(authService.updateUser(id, request));
    }

    @GetMapping("/doctors/simple")
    public ResponseEntity<List<UserSimpleDto>> getDoctorsSimple() {
        return ResponseEntity.ok(authService.getDoctorsSimple());
    }

    @PostMapping("/doctors-search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DoctorSearchResponseDto>> searchDoctors(@RequestBody DoctorSearchRequest request) {
        Page<DoctorSearchResponseDto> resultPage = authService.searchDoctors(request);
        return ResponseEntity.ok(resultPage);
    }
}