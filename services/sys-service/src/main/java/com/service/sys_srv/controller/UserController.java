package com.service.sys_srv.controller;

import com.service.sys_srv.dto.request.DoctorSearchRequest;
import com.service.sys_srv.dto.request.PatientSearchRequest;
import com.service.sys_srv.dto.request.StaffSearchRequest;
import com.service.sys_srv.dto.request.UpdateUserRequest;
import com.service.sys_srv.dto.response.*;
import com.service.sys_srv.entity.User;
import com.service.sys_srv.service.AuthService;
import com.service.sys_srv.service.JwtService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
//@PreAuthorize("isAuthenticated()")
public class UserController {
    private final AuthService authService;
    private final JwtService jwtService;

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

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorDto>> getDoctors() {
        return ResponseEntity.ok(authService.getDoctors());
    }

    @PostMapping("/staffs/search")
    public ResponseEntity<Page<StaffSearchResponseDto>> searchStaffs(
            @RequestBody StaffSearchRequest request
    ) {
        Page<StaffSearchResponseDto> resultPage = authService.searchStaffs(request);
        return ResponseEntity.ok(resultPage);
    }

    @PostMapping("/patients/search")
    public ResponseEntity<Page<PatientSearchResponseDto>> searchPatients(
            @RequestBody PatientSearchRequest request
    ) {
        Page<PatientSearchResponseDto> resultPage = authService.searchPatients(request);
        return ResponseEntity.ok(resultPage);
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

    @PostMapping("/doctors/search")
    public ResponseEntity<Page<DoctorSearchResponseDto>> searchDoctors(
            @RequestBody DoctorSearchRequest request // Nhận DTO request mới
    ) {
        Page<DoctorSearchResponseDto> resultPage = authService.searchDoctors(request);
        return ResponseEntity.ok(resultPage);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> toggleUserStatus(@PathVariable UUID id) {
        UserDto updatedUser = authService.toggleUserActiveStatus(id);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Example: GET /api/users/search-by-name?name=John&role=PATIENT
     * Returns list of UUIDs of users matching role + name contains (case-insensitive)
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<List<UUID>> searchByNameAndRole(
            @RequestParam("name") String name,
            @RequestParam("role") String role) {

        List<UUID> ids = authService.searchUserIdsByNameAndRole(name, role);
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/basic-info")
    public Map<UUID, String> getPatientNames(@RequestParam List<UUID> ids) {
        return authService.getPatientNames(ids);
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserDto> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        try {
            // Lấy token từ Authorization header
            String authHeader = request.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            
            // Lấy userId từ token
            if (token == null) {
                return ResponseEntity.status(401).body(null); // Unauthorized
            }
            
            String userIdFromToken = jwtService.extractUserId(token);
            if (userIdFromToken == null) {
                return ResponseEntity.status(401).body(null); // Unauthorized
            }
            
            UUID userId = UUID.fromString(userIdFromToken);
            UserDto updatedUser = authService.updateUserAvatar(userId, file);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/getId")
    public ResponseEntity<String> getMethodName(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }

        // Lấy userId từ token
            if (token == null) {
                return ResponseEntity.status(401).body(null); // Unauthorized
            }
            
            String userIdFromToken = jwtService.extractUserId(token);
            if (userIdFromToken == null) {
                return ResponseEntity.status(401).body(null); // Unauthorized
            }
            
            UUID userId = UUID.fromString(userIdFromToken);
        return ResponseEntity.ok(userId.toString());
    }
    
}