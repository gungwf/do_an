package com.service.sys_srv.controller;

import com.service.sys_srv.dto.request.LoginRequest;
import com.service.sys_srv.dto.request.RegisterRequest;
import com.service.sys_srv.dto.response.LoginResponse;
import com.service.sys_srv.entity.Enum.UserRole;
import com.service.sys_srv.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.registerPatient(registerRequest);
            return ResponseEntity.ok("Patient registered successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/staff")
//    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> registerStaff(@RequestBody RegisterRequest registerRequest) {
        try {
            UserRole role = UserRole.valueOf(registerRequest.getRole().toLowerCase());
            authService.registerStaff(registerRequest, role);
            return ResponseEntity.ok("Staff registered successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest);
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }


}