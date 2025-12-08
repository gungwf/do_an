package com.reporting_service.reporting_service.service.export;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.reporting_service.reporting_service.dto.response.RevenueReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    public byte[] exportToPdf(RevenueReportDto report) throws DocumentException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("BÁNG CÁO TÀI CHÍNH", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" ")); // Spacing

            // Period
            Font infoFont = new Font(Font.FontFamily.HELVETICA, 11);
            Paragraph period = new Paragraph("Khoảng thời gian: " + report.period(), infoFont);
            document.add(period);

            document.add(new Paragraph(" ")); // Spacing

            // Main metrics table
            PdfPTable metricsTable = new PdfPTable(2);
            metricsTable.setWidthPercentage(100);

            addTableHeader(metricsTable, "Chỉ số chính", "Giá trị");
            addMetricRow(metricsTable, "Tổng doanh thu", formatCurrency(report.totalRevenue()));
            addMetricRow(metricsTable, "Tổng số hóa đơn", String.valueOf(report.totalBills()));
            addMetricRow(metricsTable, "Đã thanh toán", String.valueOf(report.paidBills()));
            addMetricRow(metricsTable, "Chưa thanh toán", String.valueOf(report.pendingBills()));
            addMetricRow(metricsTable, "Tỷ lệ thanh toán (%)", String.format("%.2f%%", report.paymentRate()));

            document.add(metricsTable);
            document.add(new Paragraph(" "));

            // Revenue by bill type
            if (!report.revenueByBillType().isEmpty()) {
                PdfPTable billTypeTable = new PdfPTable(2);
                billTypeTable.setWidthPercentage(100);
                addTableHeader(billTypeTable, "Loại hóa đơn", "Doanh thu");

                for (Map.Entry<String, BigDecimal> entry : report.revenueByBillType().entrySet()) {
                    addMetricRow(billTypeTable, entry.getKey(), formatCurrency(entry.getValue()));
                }

                document.add(new Paragraph("Doanh thu theo loại hóa đơn", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
                document.add(billTypeTable);
                document.add(new Paragraph(" "));
            }

            // Revenue by branch
            if (!report.revenueByBranch().isEmpty()) {
                PdfPTable branchTable = new PdfPTable(2);
                branchTable.setWidthPercentage(100);
                addTableHeader(branchTable, "Chi nhánh", "Doanh thu");

                for (Map.Entry<String, BigDecimal> entry : report.revenueByBranch().entrySet()) {
                    addMetricRow(branchTable, "Chi nhánh: " + entry.getKey(), formatCurrency(entry.getValue()));
                }

                document.add(new Paragraph("Doanh thu theo chi nhánh", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
                document.add(branchTable);
            }

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Error exporting to PDF", e);
            throw e;
        }
    }

    private void addTableHeader(PdfPTable table, String header1, String header2) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
        
        PdfPCell cell1 = new PdfPCell(new Phrase(header1, headerFont));
        cell1.setBackgroundColor(BaseColor.BLUE);
        cell1.setPadding(8);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(header2, headerFont));
        cell2.setBackgroundColor(BaseColor.BLUE);
        cell2.setPadding(8);
        table.addCell(cell2);
    }

    private void addMetricRow(PdfPTable table, String label, String value) {
        Font contentFont = new Font(Font.FontFamily.HELVETICA, 10);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, contentFont));
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, contentFont));
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount) + " VND";
    }
}
