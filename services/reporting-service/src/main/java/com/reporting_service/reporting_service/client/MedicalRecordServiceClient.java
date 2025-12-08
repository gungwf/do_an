package com.reporting_service.reporting_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "medical-record-service", url = "${medical.record.service.url:http://localhost:8083}")
public interface MedicalRecordServiceClient {

    @GetMapping("/bills/search")
    List<BillSearchItemDto> searchBills(
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) UUID branchId
    );
}
