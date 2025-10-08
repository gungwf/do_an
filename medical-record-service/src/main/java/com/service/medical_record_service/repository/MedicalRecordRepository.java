package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    // Tìm bệnh án theo ID của lịch hẹn
    Optional<MedicalRecord> findByAppointmentId(UUID appointmentId);
}