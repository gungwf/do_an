package com.reporting_service.reporting_service.service.export;

import com.reporting_service.reporting_service.dto.response.RevenueReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportService {

    public byte[] exportToExcel(RevenueReportDto report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Revenue Report");
            
            // Set column widths
            sheet.setColumnWidth(0, 5000);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 4000);

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            int rowNum = 0;

            // Title
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁNG CÁO TÀI CHÍNH");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));

            // Period
            rowNum++;
            Row periodRow = sheet.createRow(rowNum++);
            periodRow.createCell(0).setCellValue("Khoảng thời gian:");
            periodRow.createCell(1).setCellValue(report.period());

            rowNum++;

            // Main metrics section
            Row header1 = sheet.createRow(rowNum++);
            header1.createCell(0).setCellValue("Chỉ số chính");
            header1.createCell(1).setCellValue("Giá trị");
            header1.getCell(0).setCellStyle(headerStyle);
            header1.getCell(1).setCellStyle(headerStyle);

            addMetricRow(sheet, rowNum++, "Tổng doanh thu", report.totalRevenue(), numberStyle);
            addMetricRow(sheet, rowNum++, "Tổng số hóa đơn", report.totalBills(), numberStyle);
            addMetricRow(sheet, rowNum++, "Đã thanh toán", report.paidBills(), numberStyle);
            addMetricRow(sheet, rowNum++, "Chưa thanh toán", report.pendingBills(), numberStyle);
            addMetricRow(sheet, rowNum++, "Tỷ lệ thanh toán (%)", report.paymentRate(), numberStyle);

            rowNum++;

            // Revenue by bill type
            if (!report.revenueByBillType().isEmpty()) {
                Row header2 = sheet.createRow(rowNum++);
                header2.createCell(0).setCellValue("Doanh thu theo loại hóa đơn");
                header2.createCell(1).setCellValue("Số tiền");
                header2.getCell(0).setCellStyle(headerStyle);
                header2.getCell(1).setCellStyle(headerStyle);

                for (Map.Entry<String, BigDecimal> entry : report.revenueByBillType().entrySet()) {
                    addMetricRow(sheet, rowNum++, entry.getKey(), entry.getValue(), numberStyle);
                }

                rowNum++;
            }

            // Revenue by branch
            if (!report.revenueByBranch().isEmpty()) {
                Row header3 = sheet.createRow(rowNum++);
                header3.createCell(0).setCellValue("Doanh thu theo chi nhánh");
                header3.createCell(1).setCellValue("Số tiền");
                header3.getCell(0).setCellStyle(headerStyle);
                header3.getCell(1).setCellStyle(headerStyle);

                for (Map.Entry<String, BigDecimal> entry : report.revenueByBranch().entrySet()) {
                    addMetricRow(sheet, rowNum++, "Chi nhánh: " + entry.getKey(), entry.getValue(), numberStyle);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error exporting to Excel", e);
            throw e;
        }
    }

    private void addMetricRow(Sheet sheet, int rowNum, String label, Object value, CellStyle numberStyle) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        
        Cell valueCell = row.createCell(1);
        if (value instanceof BigDecimal) {
            valueCell.setCellValue(((BigDecimal) value).doubleValue());
            valueCell.setCellStyle(numberStyle);
        } else if (value instanceof Integer) {
            valueCell.setCellValue((Integer) value);
            valueCell.setCellStyle(numberStyle);
        } else if (value instanceof Double) {
            valueCell.setCellValue((Double) value);
            valueCell.setCellStyle(numberStyle);
        } else {
            valueCell.setCellValue(value.toString());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }
}
