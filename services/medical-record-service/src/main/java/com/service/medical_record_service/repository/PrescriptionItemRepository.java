package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {
}
