package com.service.appointment_service.service;

import com.service.appointment_service.client.MedicalServiceClient;
import com.service.appointment_service.client.ProtocolDto;
import com.service.appointment_service.dto.ProtocolTrackingResponseDto;
import com.service.appointment_service.dto.StartProtocolRequest;
import com.service.appointment_service.entity.ProtocolTracking;
import com.service.appointment_service.repository.ProtocolTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProtocolTrackingService {

    private final ProtocolTrackingRepository protocolTrackingRepository;
    private final MedicalServiceClient medicalServiceClient;

    @Transactional
    public ProtocolTracking startProtocol(UUID patientId, UUID protocolId) {
        ProtocolDto protocolDto = medicalServiceClient.getProtocolById(protocolId);

        // (check xem bệnh nhân đã có liệu trình này chưa)

        ProtocolTracking tracking = new ProtocolTracking();
        tracking.setPatientId(patientId);
        tracking.setProtocolServiceId(protocolDto.id());
        tracking.setTotalSessions(protocolDto.totalSessions());
        tracking.setCompletedSessions(0); // Bắt đầu với 0

        return protocolTrackingRepository.save(tracking);
    }

    public List<ProtocolTrackingResponseDto> getTrackingForPatient(UUID patientId) {
        // 1. Tìm tất cả các bản ghi theo dõi của bệnh nhân
        List<ProtocolTracking> trackings = protocolTrackingRepository.findAllByPatientId(patientId);

        // 2. Làm giàu dữ liệu: gọi sang medical-record-service để lấy tên liệu trình
        return trackings.stream().map(tracking -> {
            // Gọi Feign client để lấy tên
            ProtocolDto protocolInfo = medicalServiceClient.getProtocolById(tracking.getProtocolServiceId());

            // Map sang DTO để trả về
            return new ProtocolTrackingResponseDto(
                    tracking.getId(),
                    tracking.getProtocolServiceId(),
                    protocolInfo.protocolName(),
                    tracking.getTotalSessions(),
                    tracking.getCompletedSessions(),
                    tracking.getStatus()
            );
        }).collect(Collectors.toList());
    }
}