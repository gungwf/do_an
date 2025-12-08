package com.reporting_service.reporting_service.dto.response;

import java.math.BigDecimal;

public record TopProductDto(
        String productName,
        Integer quantity,
        BigDecimal revenue,
        Double percentageOfTotal
) {}
