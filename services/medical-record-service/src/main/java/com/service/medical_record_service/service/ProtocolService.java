package com.service.medical_record_service.service;

import com.service.medical_record_service.entity.Protocol;
import com.service.medical_record_service.entity.ProtocolServiceId;
import com.service.medical_record_service.entity.ProtocolServiceLink;
import com.service.medical_record_service.repository.ProtocolRepository;
import com.service.medical_record_service.repository.ProtocolServiceLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.service.medical_record_service.dto.response.ProtocolResponseDto;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProtocolService {

    private final ProtocolRepository protocolRepository;
    private final ProtocolServiceLinkRepository protocolServiceLinkRepository;

    public Protocol createProtocol(Protocol protocol) {
        return protocolRepository.save(protocol);
    }

    public Page<ProtocolResponseDto> getAllProtocols(Pageable pageable, String name, Sort sort) {
        // Ensure pageable uses provided sort if any
        Pageable effectivePageable = pageable;
        if (sort != null && sort.isSorted()) {
            effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }

        Page<Protocol> page;
        if (name == null || name.isBlank()) {
            page = protocolRepository.findAll(effectivePageable);
        } else {
            page = protocolRepository.findByProtocolNameContainingIgnoreCase(name, effectivePageable);
        }

        var dtoList = page.stream().map(p -> new ProtocolResponseDto(
            p.getId(),
            p.getProtocolName(),
            p.getDescription(),
            p.getTotalSessions(),
            p.getPrice(),
            p.isActive(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        )).toList();
        return new PageImpl<>(dtoList, effectivePageable, page.getTotalElements());
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


    public List<ProtocolServiceLink> linkServicesToProtocol(UUID protocolId, List<UUID> serviceIds) {
        List<ProtocolServiceLink> links = new ArrayList<>();
        if (serviceIds == null || serviceIds.isEmpty()) {
            return links;
        }
        for (UUID serviceId : serviceIds) {
            ProtocolServiceId id = new ProtocolServiceId();
            id.setProtocolId(protocolId);
            id.setServiceId(serviceId);

            ProtocolServiceLink link = new ProtocolServiceLink();
            link.setId(id);

            links.add(protocolServiceLinkRepository.save(link));
        }
        return links;
    }
}