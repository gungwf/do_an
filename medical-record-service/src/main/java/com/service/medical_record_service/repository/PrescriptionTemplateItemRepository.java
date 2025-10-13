package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.PrescriptionTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionTemplateItemRepository extends JpaRepository<PrescriptionTemplateItem, UUID> {
}
