package com.subliminalsearch.simpleprojectresourcemanager.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service to import Purchase Orders from company spreadsheet
 * Handles VPN connectivity and local caching
 */
public class POSpreadsheetImportService {
    private static final Logger logger = LoggerFactory.getLogger(POSpreadsheetImportService.class);
    
    private static final String NETWORK_PO_PATH = "G:\\Common - Administrative\\Administrative & Project Documents\\Financial\\Purchase Order Log Book.xls";
    private static final String SHEET_NAME = "PO LogBook";
    
    private final DataSource dataSource;
    
    public POSpreadsheetImportService(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeTable();
    }
    
    private void initializeTable() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS company_purchase_orders (
                import_id INTEGER PRIMARY KEY AUTOINCREMENT,
                po_number TEXT,
                po_date TEXT,
                vendor TEXT,
                description TEXT,
                amount REAL,
                project_number TEXT,
                project_name TEXT,
                requested_by TEXT,
                approved_by TEXT,
                status TEXT,
                invoice_number TEXT,
                invoice_date TEXT,
                payment_status TEXT,
                notes TEXT,
                row_number INTEGER,
                import_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_modified TEXT
            )
        """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            logger.info("Company purchase orders table initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize company_purchase_orders table", e);
        }
    }
    
    /**
     * Check if the network PO spreadsheet is accessible (VPN connected)
     */
    public boolean isNetworkAccessible() {
        File networkFile = new File(NETWORK_PO_PATH);
        return networkFile.exists() && networkFile.canRead();
    }
    
    /**
     * Import PO data from spreadsheet
     * @return ImportResult with status and statistics
     */
    public ImportResult importPurchaseOrders() {
        ImportResult result = new ImportResult();
        
        // Check network access
        if (!isNetworkAccessible()) {
            result.success = false;
            result.message = "Cannot access network spreadsheet. Please connect to company VPN and try again.";
            logger.warn("Network PO spreadsheet not accessible at: {}", NETWORK_PO_PATH);
            return result;
        }
        
        // Copy to temp location to avoid locking issues
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("PO_LogBook_", ".xls");
            Files.copy(Paths.get(NETWORK_PO_PATH), tempFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Copied PO spreadsheet to temp location: {}", tempFile);
            
            // Read the Excel file
            List<PORecord> records = readExcelFile(tempFile.toFile());
            result.totalRows = records.size();
            
            // Clear existing data and insert new records
            clearExistingData();
            int imported = insertRecords(records);
            result.importedRows = imported;
            
            result.success = true;
            result.message = String.format("Successfully imported %d PO records from spreadsheet", imported);
            result.lastImportTime = LocalDateTime.now();
            
            logger.info("PO import completed: {} records imported", imported);
            
        } catch (Exception e) {
            result.success = false;
            result.message = "Error importing PO data: " + e.getMessage();
            logger.error("Failed to import PO spreadsheet", e);
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    logger.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
        
        return result;
    }
    
    private List<PORecord> readExcelFile(File file) throws IOException {
        List<PORecord> records = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new HSSFWorkbook(fis)) {
            
            // Try to find the sheet - handle case variations
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                // Try case-insensitive search
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    String sheetName = workbook.getSheetName(i);
                    if (sheetName.equalsIgnoreCase(SHEET_NAME) || 
                        sheetName.toLowerCase().contains("logbook")) {
                        sheet = workbook.getSheetAt(i);
                        logger.info("Found sheet: {}", sheetName);
                        break;
                    }
                }
            }
            
            if (sheet == null) {
                // Log available sheets for debugging
                logger.warn("Sheet '{}' not found. Available sheets:", SHEET_NAME);
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    logger.warn("  Sheet {}: {}", i, workbook.getSheetName(i));
                }
                throw new IOException("Sheet '" + SHEET_NAME + "' not found in workbook");
            }
            
            // Assume first row is headers, start from row 2
            int rowNum = 0;
            for (Row row : sheet) {
                rowNum++;
                if (rowNum == 1) continue; // Skip header row
                
                PORecord record = new PORecord();
                record.rowNumber = rowNum;
                
                // Read cells - adjust column indices based on actual spreadsheet structure
                // These are common columns - will need adjustment based on actual file
                record.poNumber = getCellStringValue(row.getCell(0));
                record.poDate = getCellDateValue(row.getCell(1));
                record.vendor = getCellStringValue(row.getCell(2));
                record.description = getCellStringValue(row.getCell(3));
                record.amount = getCellNumericValue(row.getCell(4));
                record.projectNumber = getCellStringValue(row.getCell(5));
                record.projectName = getCellStringValue(row.getCell(6));
                record.requestedBy = getCellStringValue(row.getCell(7));
                record.approvedBy = getCellStringValue(row.getCell(8));
                record.status = getCellStringValue(row.getCell(9));
                record.invoiceNumber = getCellStringValue(row.getCell(10));
                record.invoiceDate = getCellDateValue(row.getCell(11));
                record.paymentStatus = getCellStringValue(row.getCell(12));
                record.notes = getCellStringValue(row.getCell(13));
                
                // Only add if there's meaningful data (at least PO number or vendor)
                if (record.poNumber != null || record.vendor != null) {
                    records.add(record);
                }
            }
            
            logger.info("Read {} PO records from Excel file", records.size());
        }
        
        return records;
    }
    
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }
    
    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                // Try to parse string as date
                String dateStr = cell.getStringCellValue().trim();
                if (!dateStr.isEmpty()) {
                    // Simple date parsing - may need adjustment based on format
                    return LocalDate.parse(dateStr);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse date from cell: {}", cell, e);
        }
        
        return null;
    }
    
    private Double getCellNumericValue(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    // Remove currency symbols and commas
                    value = value.replaceAll("[,$]", "").trim();
                    if (!value.isEmpty()) {
                        return Double.parseDouble(value);
                    }
                    break;
                case FORMULA:
                    return cell.getNumericCellValue();
            }
        } catch (Exception e) {
            logger.debug("Could not parse numeric value from cell: {}", cell, e);
        }
        
        return null;
    }
    
    private void clearExistingData() {
        String sql = "DELETE FROM company_purchase_orders";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int deleted = stmt.executeUpdate();
            logger.info("Cleared {} existing PO records", deleted);
        } catch (Exception e) {
            logger.error("Failed to clear existing PO data", e);
        }
    }
    
    private int insertRecords(List<PORecord> records) {
        String sql = """
            INSERT INTO company_purchase_orders (
                po_number, po_date, vendor, description, amount,
                project_number, project_name, requested_by, approved_by,
                status, invoice_number, invoice_date, payment_status,
                notes, row_number
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        int count = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (PORecord record : records) {
                stmt.setString(1, record.poNumber);
                stmt.setString(2, record.poDate != null ? record.poDate.toString() : null);
                stmt.setString(3, record.vendor);
                stmt.setString(4, record.description);
                stmt.setObject(5, record.amount);
                stmt.setString(6, record.projectNumber);
                stmt.setString(7, record.projectName);
                stmt.setString(8, record.requestedBy);
                stmt.setString(9, record.approvedBy);
                stmt.setString(10, record.status);
                stmt.setString(11, record.invoiceNumber);
                stmt.setString(12, record.invoiceDate != null ? record.invoiceDate.toString() : null);
                stmt.setString(13, record.paymentStatus);
                stmt.setString(14, record.notes);
                stmt.setInt(15, record.rowNumber);
                
                stmt.executeUpdate();
                count++;
            }
            
        } catch (Exception e) {
            logger.error("Failed to insert PO records", e);
        }
        
        return count;
    }
    
    /**
     * Get all cached PO records
     */
    public List<PORecord> getCachedPurchaseOrders() {
        List<PORecord> records = new ArrayList<>();
        String sql = "SELECT * FROM company_purchase_orders ORDER BY row_number";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                PORecord record = new PORecord();
                record.poNumber = rs.getString("po_number");
                record.vendor = rs.getString("vendor");
                record.description = rs.getString("description");
                record.amount = rs.getDouble("amount");
                record.projectNumber = rs.getString("project_number");
                record.projectName = rs.getString("project_name");
                record.status = rs.getString("status");
                record.rowNumber = rs.getInt("row_number");
                
                String poDateStr = rs.getString("po_date");
                if (poDateStr != null) {
                    try {
                        record.poDate = LocalDate.parse(poDateStr);
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }
                
                records.add(record);
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve cached PO records", e);
        }
        
        return records;
    }
    
    /**
     * Get last import timestamp
     */
    public LocalDateTime getLastImportTime() {
        String sql = "SELECT MAX(import_timestamp) as last_import FROM company_purchase_orders";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                String timestamp = rs.getString("last_import");
                if (timestamp != null) {
                    return LocalDateTime.parse(timestamp.replace(" ", "T"));
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to get last import time", e);
        }
        
        return null;
    }
    
    // Data classes
    public static class PORecord {
        public String poNumber;
        public LocalDate poDate;
        public String vendor;
        public String description;
        public Double amount;
        public String projectNumber;
        public String projectName;
        public String requestedBy;
        public String approvedBy;
        public String status;
        public String invoiceNumber;
        public LocalDate invoiceDate;
        public String paymentStatus;
        public String notes;
        public int rowNumber;
        
        @Override
        public String toString() {
            return String.format("%s - %s ($%.2f)", 
                poNumber != null ? poNumber : "N/A", 
                vendor != null ? vendor : "Unknown",
                amount != null ? amount : 0.0);
        }
    }
    
    public static class ImportResult {
        public boolean success;
        public String message;
        public int totalRows;
        public int importedRows;
        public LocalDateTime lastImportTime;
    }
}