package com.reporting_service.reporting_service.controller;

import com.reporting_service.reporting_service.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportingService reportingService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryReport() {
        return ResponseEntity.ok(reportingService.generateSummaryReport());
    }
}