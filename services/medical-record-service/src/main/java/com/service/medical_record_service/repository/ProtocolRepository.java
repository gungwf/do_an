package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.Protocol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProtocolRepository extends JpaRepository<Protocol, UUID> {
	Page<Protocol> findByProtocolNameContainingIgnoreCase(String name, Pageable pageable);
}