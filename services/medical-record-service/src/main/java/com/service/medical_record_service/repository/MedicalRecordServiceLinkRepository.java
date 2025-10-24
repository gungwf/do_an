package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.MedicalRecordServiceId;
import com.service.medical_record_service.entity.MedicalRecordServiceLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordServiceLinkRepository extends JpaRepository<MedicalRecordServiceLink, MedicalRecordServiceId> {
}