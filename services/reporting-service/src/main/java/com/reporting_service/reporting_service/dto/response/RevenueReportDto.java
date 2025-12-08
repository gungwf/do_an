package com.reporting_service.reporting_service.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RevenueReportDto(
        String period,
        BigDecimal totalRevenue,
        Integer totalBills,
        Integer paidBills,
        Integer pendingBills,
        Double paymentRate,
        Map<String, BigDecimal> revenueByBillType,
        Map<String, BigDecimal> revenueByBranch,
        List<TopProductDto> topProducts
) {}
