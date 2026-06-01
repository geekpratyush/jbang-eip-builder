package com.tessera.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for converting various document formats (PDF, Word, Excel) to Markdown.
 * Aimed at providing clean text context for LLM consumption.
 */
public class DocumentConverter {

    static {
        // Prevent false-positive "Zip bomb" detection for highly compressed Excel files
        ZipSecureFile.setMinInflateRatio(0.001);
    }

    public static String convertPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public static String convertDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    sb.append(text).append("\n\n");
                }
            }
            return sb.toString();
        }
    }

    public static String convertXlsx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            StringBuilder sb = new StringBuilder();
            sb.append("# Excel Document: ").append(file.getName()).append("\n\n");
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    sb.append("## Sheet: ").append(sheet.getSheetName()).append("\n\n");
                    sb.append(renderSheetToMarkdownTable(sheet));
                    sb.append("\n\n---\n\n");
                }
            }
            return sb.toString();
        }
    }

    private static String renderSheetToMarkdownTable(Sheet sheet) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum < 0) return "*Empty Sheet*";

        // 1. Scan for max columns and calculate column widths for better visual structure
        int maxCols = 0;
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getPhysicalNumberOfCells() > 0) {
                rows.add(row);
                maxCols = Math.max(maxCols, row.getLastCellNum());
            }
        }

        if (rows.isEmpty() || maxCols <= 0) return "*Empty Sheet*";

        int[] colWidths = new int[maxCols];
        List<String[]> data = new ArrayList<>();

        for (Row row : rows) {
            String[] rowData = new String[maxCols];
            for (int c = 0; c < maxCols; c++) {
                Cell cell = row.getCell(c);
                String val = getCellValue(cell).replace("|", "\\|").replace("\n", " ").trim();
                rowData[c] = val;
                colWidths[c] = Math.max(colWidths[c], val.length());
            }
            data.add(rowData);
        }

        // Ensure minimum width for the "---" separator
        for (int c = 0; c < maxCols; c++) {
            colWidths[c] = Math.max(colWidths[c], 3);
        }

        // 2. Build the Markdown Table
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < data.size(); r++) {
            String[] row = data.get(r);
            sb.append("|");
            for (int c = 0; c < maxCols; c++) {
                sb.append(" ").append(padRight(row[c], colWidths[c])).append(" |");
            }
            sb.append("\n");

            // Render separator after the first data row (assumed header)
            if (r == 0) {
                sb.append("|");
                for (int c = 0; c < maxCols; c++) {
                    sb.append(" ").append("-".repeat(colWidths[c])).append(" |");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
