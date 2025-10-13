package com.service.medical_record_service.controller;

import com.service.medical_record_service.dto.LinkServiceRequest;
import com.service.medical_record_service.entity.Protocol;
import com.service.medical_record_service.entity.ProtocolServiceLink;
import com.service.medical_record_service.service.ProtocolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<Protocol>> getAllProtocols() {
        return ResponseEntity.ok(protocolService.getAllProtocols());
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
    public ResponseEntity<ProtocolServiceLink> linkService(
            @PathVariable UUID protocolId,
            @RequestBody LinkServiceRequest request
    ) {
        return ResponseEntity.ok(protocolService.linkServiceToProtocol(protocolId, request.serviceId()));
    }
}