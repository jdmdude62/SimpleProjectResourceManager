import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestPOSpreadsheetReader {
    public static void main(String[] args) {
        String networkPath = "G:\\Common - Administrative\\Administrative & Project Documents\\Financial\\Purchase Order Log Book.xls";
        
        try {
            // Check if file exists
            File networkFile = new File(networkPath);
            if (!networkFile.exists()) {
                System.out.println("ERROR: Cannot access network file. Please connect to VPN.");
                return;
            }
            
            System.out.println("Network file accessible: " + networkFile.getAbsolutePath());
            System.out.println("File size: " + networkFile.length() + " bytes");
            
            // Copy to temp
            Path tempFile = Files.createTempFile("PO_LogBook_", ".xls");
            Files.copy(networkFile.toPath(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied to temp: " + tempFile);
            
            // Read Excel
            try (FileInputStream fis = new FileInputStream(tempFile.toFile());
                 Workbook workbook = new HSSFWorkbook(fis)) {
                
                System.out.println("\n=== Workbook Info ===");
                System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
                
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    System.out.println("Sheet " + i + ": " + workbook.getSheetName(i));
                }
                
                // Read PO LogBook sheet
                Sheet sheet = workbook.getSheet("PO LogBook");
                if (sheet == null) {
                    System.out.println("ERROR: Sheet 'PO LogBook' not found!");
                    return;
                }
                
                System.out.println("\n=== PO LogBook Sheet ===");
                System.out.println("Total rows: " + sheet.getPhysicalNumberOfRows());
                System.out.println("Last row num: " + sheet.getLastRowNum());
                
                // Read header row
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    System.out.println("\n=== Column Headers (Row 0) ===");
                    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                        Cell cell = headerRow.getCell(i);
                        String value = getCellValue(cell);
                        System.out.println("Column " + i + ": " + value);
                    }
                }
                
                // Read first few data rows
                System.out.println("\n=== First 5 Data Rows ===");
                for (int rowNum = 1; rowNum <= Math.min(5, sheet.getLastRowNum()); rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (row != null) {
                        System.out.println("\nRow " + rowNum + ":");
                        for (int i = 0; i < Math.min(10, row.getLastCellNum()); i++) {
                            Cell cell = row.getCell(i);
                            String value = getCellValue(cell);
                            if (value != null && !value.trim().isEmpty()) {
                                System.out.println("  Col " + i + ": " + value);
                            }
                        }
                    }
                }
                
            }
            
            // Clean up
            Files.deleteIfExists(tempFile);
            System.out.println("\nTemp file cleaned up.");
            
        } catch (Exception e) {
            System.err.println("Error reading spreadsheet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        return "FORMULA";
                    }
                }
            default:
                return "";
        }
    }
}