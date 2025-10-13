package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.ServiceMaterial;
import com.service.medical_record_service.entity.ServiceMaterialId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ServiceMaterialRepository extends JpaRepository<ServiceMaterial, ServiceMaterialId> {
    List<ServiceMaterial> findById_ServiceId(UUID serviceId);
}