package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.DiagnosisTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosisTemplateRepository extends JpaRepository<DiagnosisTemplate, UUID> {
    List<DiagnosisTemplate> findByDoctorId(UUID doctorId);
}
