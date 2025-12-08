package com.reporting_service.reporting_service.controller;

import com.reporting_service.reporting_service.dto.ApiResponse;
import com.reporting_service.reporting_service.dto.request.RevenueReportRequest;
import com.reporting_service.reporting_service.dto.response.RevenueReportDto;
import com.reporting_service.reporting_service.service.ReportingService;
import com.reporting_service.reporting_service.service.RevenueReportService;
import com.reporting_service.reporting_service.service.export.ExcelExportService;
import com.reporting_service.reporting_service.service.export.PdfExportService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {
    private final ReportingService reportingService;
    private final RevenueReportService revenueReportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryReport() {
        return ResponseEntity.ok(reportingService.generateSummaryReport());
    }

    /**
     * Lấy báng cáo tài chính
     */
    @GetMapping("/revenue")
    @Operation(summary = "Lấy bảng cáo tài chính")
    public ResponseEntity<ApiResponse<RevenueReportDto>> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) UUID branchId) {
        try {
            log.info("Getting revenue report: startDate={}, endDate={}, billType={}, branchId={}",
                    startDate, endDate, billType, branchId);

            RevenueReportRequest request = new RevenueReportRequest(startDate, endDate, billType, branchId, null);
            RevenueReportDto report = revenueReportService.generateRevenueReport(request);

            return ResponseEntity.ok(ApiResponse.success(report, "Báng cáo tài chính"));
        } catch (Exception e) {
            log.error("Error getting revenue report", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate revenue report"));
        }
    }

    /**
     * Xuất báng cáo tài chính sang Excel
     */
    @GetMapping("/revenue/export/excel")
    @Operation(summary = "Xuất báng cáo tài chính sang Excel")
    public ResponseEntity<byte[]> exportRevenueToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) UUID branchId) {
        try {
            log.info("Exporting revenue report to Excel");

            RevenueReportRequest request = new RevenueReportRequest(startDate, endDate, billType, branchId, null);
            RevenueReportDto report = revenueReportService.generateRevenueReport(request);

            byte[] excelBytes = excelExportService.exportToExcel(report);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"revenue_report_" + System.currentTimeMillis() + ".xlsx\"")
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Error exporting to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất báng cáo tài chính sang PDF
     */
    @GetMapping("/revenue/export/pdf")
    @Operation(summary = "Xuất báng cáo tài chính sang PDF")
    public ResponseEntity<byte[]> exportRevenueToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) UUID branchId) {
        try {
            log.info("Exporting revenue report to PDF");

            RevenueReportRequest request = new RevenueReportRequest(startDate, endDate, billType, branchId, null);
            RevenueReportDto report = revenueReportService.generateRevenueReport(request);

            byte[] pdfBytes = pdfExportService.exportToPdf(report);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"revenue_report_" + System.currentTimeMillis() + ".pdf\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error exporting to PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}