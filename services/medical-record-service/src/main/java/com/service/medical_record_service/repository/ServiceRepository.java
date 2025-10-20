package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, UUID> {
    Optional<Service> findById(UUID id);
}