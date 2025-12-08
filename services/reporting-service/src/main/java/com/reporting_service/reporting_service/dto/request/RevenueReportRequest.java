package com.reporting_service.reporting_service.dto.request;

import java.time.Instant;
import java.util.UUID;

public record RevenueReportRequest(
        Instant startDate,
        Instant endDate,
        String billType,
        UUID branchId,
        String exportFormat
) {}
