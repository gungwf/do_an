package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.request.LinkServicesRequest;
import com.service.medical_record_service.dto.response.ProtocolResponseDto;
import com.service.medical_record_service.entity.Protocol;
import com.service.medical_record_service.entity.ProtocolServiceLink;
import com.service.medical_record_service.service.ProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolService protocolService;

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Protocol> createProtocol(@RequestBody Protocol protocol) {
        return ResponseEntity.ok(protocolService.createProtocol(protocol));
    }

    @GetMapping
    public ResponseEntity<Page<ProtocolResponseDto>> getAllProtocols(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                sortObj = Sort.by(Sort.Direction.fromString(parts[1].trim()), parts[0].trim());
            } else {
                sortObj = Sort.by(sort.trim());
            }
        }
        return ResponseEntity.ok(protocolService.getAllProtocols(PageRequest.of(page, size), name, sortObj));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Protocol> getProtocolById(@PathVariable UUID id) {
        return ResponseEntity.ok(protocolService.getProtocolById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Protocol> updateProtocol(@PathVariable UUID id, @RequestBody Protocol protocolDetails) {
        return ResponseEntity.ok(protocolService.updateProtocol(id, protocolDetails));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<Void> deleteProtocol(@PathVariable UUID id) {
        protocolService.deleteProtocol(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{protocolId}/services")
    @PreAuthorize("hasAuthority('admin')")
    public ResponseEntity<List<ProtocolServiceLink>> linkServices(
            @PathVariable UUID protocolId,
            @RequestBody LinkServicesRequest request
    ) {
        return ResponseEntity.ok(protocolService.linkServicesToProtocol(protocolId, request.serviceIds()));
    }
}