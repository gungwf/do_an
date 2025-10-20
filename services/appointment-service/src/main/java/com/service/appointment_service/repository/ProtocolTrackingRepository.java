package com.service.appointment_service.repository;

import com.service.appointment_service.entity.Enum.ProtocolStatus;
import com.service.appointment_service.entity.ProtocolTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProtocolTrackingRepository extends JpaRepository<ProtocolTracking, UUID> {
    Optional<ProtocolTracking> findByPatientIdAndProtocolServiceIdAndStatus(
            UUID patientId, UUID protocolServiceId, ProtocolStatus status);

    List<ProtocolTracking> findAllByPatientId(UUID patientId);
}
