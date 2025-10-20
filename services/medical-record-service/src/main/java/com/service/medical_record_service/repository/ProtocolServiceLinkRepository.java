package com.service.medical_record_service.repository;

import com.service.medical_record_service.entity.ProtocolServiceId;
import com.service.medical_record_service.entity.ProtocolServiceLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtocolServiceLinkRepository extends JpaRepository<ProtocolServiceLink, ProtocolServiceId> {}