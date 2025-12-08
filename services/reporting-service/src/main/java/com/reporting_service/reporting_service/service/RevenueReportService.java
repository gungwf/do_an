package com.reporting_service.reporting_service.service;

import com.reporting_service.reporting_service.client.BillSearchItemDto;
import com.reporting_service.reporting_service.client.MedicalRecordServiceClient;
import com.reporting_service.reporting_service.dto.request.RevenueReportRequest;
import com.reporting_service.reporting_service.dto.response.RevenueReportDto;
import com.reporting_service.reporting_service.dto.response.TopProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueReportService {

    private final MedicalRecordServiceClient medicalRecordServiceClient;

    public RevenueReportDto generateRevenueReport(RevenueReportRequest request) {
        try {
            log.info("Generating revenue report for period: {} to {}", request.startDate(), request.endDate());

            // Lấy dữ liệu bill từ medical-record-service
            List<BillSearchItemDto> bills = medicalRecordServiceClient.searchBills(
                    request.billType(),
                    "PAID",
                    request.startDate(),
                    request.endDate(),
                    request.branchId()
            );

            if (bills == null || bills.isEmpty()) {
                return createEmptyReport(request);
            }

            // Tính toán các chỉ số chính
            BigDecimal totalRevenue = bills.stream()
                    .map(BillSearchItemDto::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Lấy tất cả bills (không chỉ PAID) để tính tỷ lệ
            List<BillSearchItemDto> allBills = medicalRecordServiceClient.searchBills(
                    request.billType(),
                    null,
                    request.startDate(),
                    request.endDate(),
                    request.branchId()
            );

            Integer totalBills = allBills.size();
            Integer paidBills = bills.size();
            Integer pendingBills = totalBills - paidBills;
            Double paymentRate = totalBills > 0 ? (paidBills * 100.0 / totalBills) : 0.0;

            // Phân tích theo loại bill
            Map<String, BigDecimal> revenueByBillType = bills.stream()
                    .collect(Collectors.groupingBy(
                            BillSearchItemDto::billType,
                            Collectors.reducing(BigDecimal.ZERO,
                                    BillSearchItemDto::totalAmount,
                                    BigDecimal::add)
                    ));

            // Phân tích theo nhánh
            Map<String, BigDecimal> revenueByBranch = bills.stream()
                    .collect(Collectors.groupingBy(
                            bill -> bill.branchId().toString(),
                            Collectors.reducing(BigDecimal.ZERO,
                                    BillSearchItemDto::totalAmount,
                                    BigDecimal::add)
                    ));

            String period = formatPeriod(request.startDate(), request.endDate());

            return new RevenueReportDto(
                    period,
                    totalRevenue,
                    totalBills,
                    paidBills,
                    pendingBills,
                    Math.round(paymentRate * 100.0) / 100.0,
                    revenueByBillType,
                    revenueByBranch,
                    List.of() // TODO: Thêm logic lấy top products từ bill items
            );

        } catch (Exception e) {
            log.error("Error generating revenue report", e);
            throw new RuntimeException("Failed to generate revenue report: " + e.getMessage());
        }
    }

    private RevenueReportDto createEmptyReport(RevenueReportRequest request) {
        return new RevenueReportDto(
                formatPeriod(request.startDate(), request.endDate()),
                BigDecimal.ZERO,
                0,
                0,
                0,
                0.0,
                new HashMap<>(),
                new HashMap<>(),
                new ArrayList<>()
        );
    }

    private String formatPeriod(Instant startDate, Instant endDate) {
        if (startDate == null || endDate == null) {
            return "All time";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
        return formatter.format(startDate) + " to " + formatter.format(endDate);
    }
}
