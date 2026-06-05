package com.example;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.BindToRegistry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class ReportingService {
    private static final Logger LOG = LoggerFactory.getLogger(ReportingService.class);
    private static final String H2_URL = "jdbc:h2:mem:integration_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @BindToRegistry("reportingService")
    public ReportingService getService() {
        return this;
    }

    public Map<String, Object> getRecords(int page, int size, String sortColumn, String sortOrder, String filterText) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        int total = 0;
        
        // Validate sort column to avoid SQL injection
        Set<String> validCols = new HashSet<>(Arrays.asList("id", "counterparty", "transaction_type", "amount", "currency", "status", "created_at"));
        String sort = validCols.contains(sortColumn) ? sortColumn : "created_at";
        String order = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        
        int offset = (page - 1) * size;
        
        String baseQuery = "FROM chassis.transactions WHERE 1=1 ";
        if (filterText != null && !filterText.trim().isEmpty()) {
            baseQuery += "AND (LOWER(counterparty) LIKE ? OR LOWER(transaction_type) LIKE ? OR LOWER(status) LIKE ? OR LOWER(id) LIKE ?)";
        }
        
        try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "")) {
            // Count total
            String countSql = "SELECT COUNT(*) " + baseQuery;
            try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
                if (filterText != null && !filterText.trim().isEmpty()) {
                    String param = "%" + filterText.toLowerCase() + "%";
                    pstmt.setString(1, param);
                    pstmt.setString(2, param);
                    pstmt.setString(3, param);
                    pstmt.setString(4, param);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt(1);
                    }
                }
            }
            
            // Query paginated data
            String dataSql = "SELECT id, counterparty, transaction_type, amount, currency, status, created_at " 
                    + baseQuery 
                    + "ORDER BY " + sort + " " + order + " LIMIT ? OFFSET ?";
            try (PreparedStatement pstmt = conn.prepareStatement(dataSql)) {
                int pIdx = 1;
                if (filterText != null && !filterText.trim().isEmpty()) {
                    String param = "%" + filterText.toLowerCase() + "%";
                    pstmt.setString(pIdx++, param);
                    pstmt.setString(pIdx++, param);
                    pstmt.setString(pIdx++, param);
                    pstmt.setString(pIdx++, param);
                }
                pstmt.setInt(pIdx++, size);
                pstmt.setInt(pIdx++, offset);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", rs.getString("id"));
                        row.put("counterparty", rs.getString("counterparty"));
                        row.put("transaction_type", rs.getString("transaction_type"));
                        row.put("amount", rs.getDouble("amount"));
                        row.put("currency", rs.getString("currency"));
                        row.put("status", rs.getString("status"));
                        row.put("created_at", rs.getTimestamp("created_at").toString());
                        data.add(row);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to query records", e);
        }
        
        result.put("total", total);
        result.put("data", data);
        return result;
    }

    public byte[] exportCsv(String filterText) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("Transaction ID,Counterparty,Type,Amount,Currency,Status,Created At");
            
            String query = "SELECT id, counterparty, transaction_type, amount, currency, status, created_at FROM chassis.transactions WHERE 1=1 ";
            if (filterText != null && !filterText.trim().isEmpty()) {
                query += "AND (LOWER(counterparty) LIKE ? OR LOWER(transaction_type) LIKE ? OR LOWER(status) LIKE ?)";
            }
            query += " ORDER BY created_at DESC LIMIT 5000"; // limit export size for safety
            
            try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "");
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                if (filterText != null && !filterText.trim().isEmpty()) {
                    String param = "%" + filterText.toLowerCase() + "%";
                    pstmt.setString(1, param);
                    pstmt.setString(2, param);
                    pstmt.setString(3, param);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        writer.println(String.format("%s,\"%s\",%s,%.2f,%s,%s,%s",
                            rs.getString("id"),
                            rs.getString("counterparty").replace("\"", "\"\""),
                            rs.getString("transaction_type"),
                            rs.getDouble("amount"),
                            rs.getString("currency"),
                            rs.getString("status"),
                            rs.getTimestamp("created_at").toString()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("CSV generation failed", e);
        }
        return out.toByteArray();
    }

    public byte[] exportExcel(String filterText) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");
            
            // Header font and style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Transaction ID", "Counterparty", "Type", "Amount", "Currency", "Status", "Created At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            String query = "SELECT id, counterparty, transaction_type, amount, currency, status, created_at FROM chassis.transactions WHERE 1=1 ";
            if (filterText != null && !filterText.trim().isEmpty()) {
                query += "AND (LOWER(counterparty) LIKE ? OR LOWER(transaction_type) LIKE ? OR LOWER(status) LIKE ?)";
            }
            query += " ORDER BY created_at DESC LIMIT 5000";
            
            try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "");
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                if (filterText != null && !filterText.trim().isEmpty()) {
                    String param = "%" + filterText.toLowerCase() + "%";
                    pstmt.setString(1, param);
                    pstmt.setString(2, param);
                    pstmt.setString(3, param);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    int rowIdx = 1;
                    while (rs.next()) {
                        Row row = sheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(rs.getString("id"));
                        row.createCell(1).setCellValue(rs.getString("counterparty"));
                        row.createCell(2).setCellValue(rs.getString("transaction_type"));
                        row.createCell(3).setCellValue(rs.getDouble("amount"));
                        row.createCell(4).setCellValue(rs.getString("currency"));
                        row.createCell(5).setCellValue(rs.getString("status"));
                        row.createCell(6).setCellValue(rs.getTimestamp("created_at").toString());
                    }
                }
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            LOG.error("Excel generation failed", e);
            return new byte[0];
        }
    }

    public byte[] exportPdf(String filterText) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Title
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Modular Integration Chassis Report");
                contentStream.endText();
                
                // Subtitle
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                contentStream.newLineAtOffset(50, 735);
                contentStream.showText("Generated: " + new java.util.Date().toString() + " | Filter: " + (filterText != null ? filterText : "None"));
                contentStream.endText();
                
                // Simple table headers
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 9);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("TXN ID             Counterparty                         Type                 Amount       Status");
                contentStream.endText();
                
                // Draw a line
                contentStream.moveTo(50, 690);
                contentStream.lineTo(550, 690);
                contentStream.stroke();
                
                String query = "SELECT id, counterparty, transaction_type, amount, currency, status FROM chassis.transactions WHERE 1=1 ";
                if (filterText != null && !filterText.trim().isEmpty()) {
                    query += "AND (LOWER(counterparty) LIKE ? OR LOWER(transaction_type) LIKE ? OR LOWER(status) LIKE ?)";
                }
                query += " ORDER BY created_at DESC LIMIT 25"; // limit pdf page count
                
                try (Connection conn = DriverManager.getConnection(H2_URL, "sa", "");
                     PreparedStatement pstmt = conn.prepareStatement(query)) {
                    if (filterText != null && !filterText.trim().isEmpty()) {
                        String param = "%" + filterText.toLowerCase() + "%";
                        pstmt.setString(1, param);
                        pstmt.setString(2, param);
                        pstmt.setString(3, param);
                    }
                    try (ResultSet rs = pstmt.executeQuery()) {
                        int yOffset = 670;
                        contentStream.setFont(PDType1Font.HELVETICA, 8);
                        while (rs.next() && yOffset > 50) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(50, yOffset);
                            
                            String id = rs.getString("id");
                            String cp = rs.getString("counterparty");
                            if (cp.length() > 25) cp = cp.substring(0, 22) + "...";
                            String type = rs.getString("transaction_type");
                            double amt = rs.getDouble("amount");
                            String status = rs.getString("status");
                            
                            String line = String.format("%-14s %-30s %-18s %10.2f       %-12s", id, cp, type, amt, status);
                            contentStream.showText(line);
                            contentStream.endText();
                            yOffset -= 20;
                        }
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            LOG.error("PDF generation failed", e);
            return new byte[0];
        }
    }
}
