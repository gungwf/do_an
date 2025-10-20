package com.service.medical_record_service.service;

import com.service.medical_record_service.entity.Protocol;
import com.service.medical_record_service.entity.ProtocolServiceId;
import com.service.medical_record_service.entity.ProtocolServiceLink;
import com.service.medical_record_service.repository.ProtocolRepository;
import com.service.medical_record_service.repository.ProtocolServiceLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProtocolService {

    private final ProtocolRepository protocolRepository;
    private final ProtocolServiceLinkRepository protocolServiceLinkRepository;

    public Protocol createProtocol(Protocol protocol) {
        return protocolRepository.save(protocol);
    }

    public List<Protocol> getAllProtocols() {
        return protocolRepository.findAll();
    }

    public Protocol getProtocolById(UUID id) {
        return protocolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Protocol not found with id: " + id));
    }

    public Protocol updateProtocol(UUID id, Protocol protocolDetails) {
        Protocol existingProtocol = getProtocolById(id);
        existingProtocol.setProtocolName(protocolDetails.getProtocolName());
        existingProtocol.setDescription(protocolDetails.getDescription());
        existingProtocol.setTotalSessions(protocolDetails.getTotalSessions());
        existingProtocol.setPrice(protocolDetails.getPrice());
        existingProtocol.setActive(protocolDetails.isActive());
        return protocolRepository.save(existingProtocol);
    }

    public void deleteProtocol(UUID id) {
        Protocol existingProtocol = getProtocolById(id);
        existingProtocol.setActive(false); // Xóa mềm
        protocolRepository.save(existingProtocol);
    }

    public ProtocolServiceLink linkServiceToProtocol(UUID protocolId, UUID serviceId) {
        // (Thêm logic kiểm tra sự tồn tại của protocol và service)
        ProtocolServiceId id = new ProtocolServiceId();
        id.setProtocolId(protocolId);
        id.setServiceId(serviceId);

        ProtocolServiceLink link = new ProtocolServiceLink();
        link.setId(id);

        return protocolServiceLinkRepository.save(link);
    }
}