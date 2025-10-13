package com.service.medical_record_service.controller;

import com.service.medical_record_service.client.UserDto;
import com.service.medical_record_service.client.UserServiceClient;
import com.service.medical_record_service.dto.TemplateRequest;
import com.service.medical_record_service.entity.DiagnosisTemplate;
import com.service.medical_record_service.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;
    private final UserServiceClient userServiceClient;

    @PostMapping
//    @PreAuthorize("hasAuthority('doctor')")
    public ResponseEntity<DiagnosisTemplate> createTemplate(Authentication authentication, @RequestBody TemplateRequest request) {
        String doctorEmail = authentication.getName();
        UserDto doctor = userServiceClient.getUserByEmail(doctorEmail);
        return ResponseEntity.ok(templateService.createTemplate(doctor.id(), request));
    }

    @GetMapping("/my-templates")
    @PreAuthorize("hasAuthority('doctor')")
    public ResponseEntity<List<DiagnosisTemplate>> getMyTemplates(Authentication authentication) {
        String doctorEmail = authentication.getName();
        UserDto doctor = userServiceClient.getUserByEmail(doctorEmail);
        return ResponseEntity.ok(templateService.getTemplatesByDoctor(doctor.id()));
    }
}