package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);

    Page<MedicalRecord> findByAppointmentIdIn(List<UUID> appointmentIds, Pageable pageable);
}