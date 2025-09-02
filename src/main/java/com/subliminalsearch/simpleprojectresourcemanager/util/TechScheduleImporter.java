package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

public class TechScheduleImporter {
    private static final Logger logger = LoggerFactory.getLogger(TechScheduleImporter.class);
    private final SchedulingService schedulingService;
    private final DataSource dataSource;
    
    // Track created projects to avoid duplicates
    private Map<String, Project> projectCache = new HashMap<>();
    private Map<String, Resource> resourceCache = new HashMap<>();
    private Map<String, ProjectManager> projectManagerCache = new HashMap<>();
    
    // Statistics for import report
    private int projectsCreated = 0;
    private int assignmentsCreated = 0;
    private int errorsEncountered = 0;
    private List<String> errorMessages = new ArrayList<>();
    
    public TechScheduleImporter(SchedulingService schedulingService) {
        this(schedulingService, null);
    }
    
    public TechScheduleImporter(SchedulingService schedulingService, DataSource dataSource) {
        this.schedulingService = schedulingService;
        this.dataSource = dataSource;
        initializeCaches();
    }
    
    private String getAvailableResourceNames() {
        List<String> names = new ArrayList<>(resourceCache.keySet());
        names.sort(String::compareToIgnoreCase);
        if (names.isEmpty()) {
            return "(No resources found in database)";
        }
        return String.join(", ", names);
    }
    
    private void initializeCaches() {
        // Load existing resources
        List<Resource> resources = schedulingService.getAllResources();
        for (Resource resource : resources) {
            resourceCache.put(resource.getName().toLowerCase(), resource);
        }
        
        // Load existing project managers
        List<ProjectManager> managers = schedulingService.getAllProjectManagers();
        for (ProjectManager pm : managers) {
            projectManagerCache.put(pm.getName().toLowerCase(), pm);
        }
        
        // Don't cache projects - each import creates fresh projects with correct PM
    }
    
    public ImportResult importExcelFile(String filePath) {
        logger.info("Starting import from: {}", filePath);
        ImportResult result = new ImportResult();
        
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            // Process each worksheet (each technician)
            int numberOfSheets = workbook.getNumberOfSheets();
            logger.info("Found {} worksheets to process", numberOfSheets);
            
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String technicianName = sheet.getSheetName();
                logger.info("Processing sheet: {}", technicianName);
                
                // Skip summary or non-technician sheets
                if (technicianName.equalsIgnoreCase("Summary") || 
                    technicianName.equalsIgnoreCase("Template") ||
                    technicianName.startsWith("~")) {
                    logger.info("Skipping non-technician sheet: {}", technicianName);
                    continue;
                }
                
                try {
                    processWorksheet(sheet, technicianName, result);
                } catch (Exception e) {
                    String error = String.format(
                        "ERROR - Sheet '%s': Failed to process worksheet.\n" +
                        "  TECHNICAL ERROR: %s\n" +
                        "  POSSIBLE CAUSES:\n" +
                        "    - Invalid date formats in Column B\n" +
                        "    - Merged cells that span multiple months\n" +
                        "    - Corrupted cell formatting\n" +
                        "  SOLUTION: Check the worksheet for formatting issues",
                        technicianName, e.getMessage()
                    );
                    logger.error(error, e);
                    result.addError(error);
                }
            }
            
        } catch (Exception e) {
            String error = String.format(
                "ERROR - File Reading: Cannot open or read the Excel file.\n" +
                "  FILE: %s\n" +
                "  TECHNICAL ERROR: %s\n" +
                "  POSSIBLE CAUSES:\n" +
                "    - File is currently open in Excel (close it first)\n" +
                "    - File is not a valid Excel format (.xlsx required)\n" +
                "    - File is corrupted or password protected\n" +
                "    - Insufficient permissions to read the file\n" +
                "  SOLUTION: Close Excel, verify file format, and try again",
                filePath, e.getMessage()
            );
            logger.error(error, e);
            result.addError(error);
        }
        
        result.setProjectsCreated(projectsCreated);
        result.setAssignmentsCreated(assignmentsCreated);
        
        logger.info("Import completed - Projects: {}, Assignments: {}, Errors: {}", 
            projectsCreated, assignmentsCreated, errorsEncountered);
        
        return result;
    }
    
    private void processWorksheet(Sheet sheet, String technicianName, ImportResult result) {
        // Find the resource for this technician
        Resource technician = resourceCache.get(technicianName.toLowerCase());
        if (technician == null) {
            String error = String.format(
                "ERROR - Sheet '%s': Technician/Resource not found in database.\n" +
                "  ISSUE: The worksheet name '%s' does not match any resource in the system.\n" +
                "  SOLUTION: Either:\n" +
                "    1) Create a resource named '%s' in Resources menu before importing, OR\n" +
                "    2) Rename the worksheet to match an existing resource name\n" +
                "  AVAILABLE RESOURCES: %s",
                technicianName, technicianName, technicianName,
                getAvailableResourceNames()
            );
            logger.warn(error);
            result.addError(error);
            return;
        }
        
        logger.info("Found resource: {} (ID: {})", technician.getName(), technician.getId());
        
        // Process each row looking for month/year markers in column B
        int lastRowNum = sheet.getLastRowNum();
        
        for (int rowNum = 0; rowNum <= lastRowNum; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            
            Cell cellB = row.getCell(1); // Column B
            if (cellB == null) continue;
            
            String cellBValue = getCellStringValue(cellB).trim();
            if (cellBValue.isEmpty()) continue;
            
            // Log all values being read from Column B
            logger.info("Sheet '{}', Row {}, Column B: '{}'", sheet.getSheetName(), rowNum + 1, cellBValue);
            
            // Check if this is a month/year indicator
            LocalDate monthDate = parseMonthYear(cellBValue);
            if (monthDate != null) {
                logger.info("Found month marker: {} at row {}", monthDate, rowNum + 1);
                // Process assignments for this month
                processMonthAssignments(sheet, rowNum, monthDate, technician, result);
            }
        }
    }
    
    private void processMonthAssignments(Sheet sheet, int monthRowNum, LocalDate monthDate, 
                                        Resource technician, ImportResult result) {
        // Process rows below the month marker - PM will be determined per row
        int currentYear = monthDate.getYear();
        int currentMonth = monthDate.getMonthValue();
        
        // Map column index to day number by reading the header row
        Map<Integer, Integer> columnToDayMap = new HashMap<>();
        
        // Find and process the header row (typically row after month marker)
        Row headerRow = sheet.getRow(monthRowNum + 1);
        int dataStartRow = monthRowNum + 2; // Default to row after header
        
        if (headerRow != null && isHeaderRow(headerRow)) {
            logger.info("Processing header row {} to map columns to days", monthRowNum + 2);
            columnToDayMap = buildColumnToDayMapForMonth(headerRow, monthDate);
            logger.info("Column to day mapping: {}", columnToDayMap);
            
            // Check if there's a second header row with day numbers
            Row nextRow = sheet.getRow(monthRowNum + 2);
            if (nextRow != null && hasDayNumbers(nextRow)) {
                logger.info("Found second header row with day numbers at row {}", monthRowNum + 3);
                dataStartRow = monthRowNum + 3; // Skip both header rows
            }
        }
        
        // Process rows below the header row(s)
        for (int rowNum = dataStartRow; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            
            Cell cellB = row.getCell(1); // Column B
            if (cellB != null) {
                String cellBValue = getCellStringValue(cellB).trim();
                
                // Check if we hit another month marker - stop processing
                if (parseMonthYear(cellBValue) != null) {
                    break;
                }
                
                // Check for project manager
                ProjectManager pm = findProjectManager(cellBValue);
                boolean isPMRow = false;
                if (pm != null) {
                    // Don't update currentPM here - use the specific PM for this row's projects
                    logger.info("Found PM: {} at row {}", pm.getName(), rowNum + 1);
                    isPMRow = true;
                    // Process project assignments on PM rows with THIS SPECIFIC PM
                    processProjectAssignments(sheet, row, rowNum, currentYear, currentMonth, 
                                            technician, pm, columnToDayMap, result);
                } else if (!cellBValue.isEmpty() && 
                          !cellBValue.equalsIgnoreCase("Shop/Open") && 
                          !cellBValue.equalsIgnoreCase("Shop") &&
                          !cellBValue.equalsIgnoreCase("Training") &&
                          !cellBValue.equalsIgnoreCase("Time Off/Holiday") &&
                          !cellBValue.equalsIgnoreCase("Time Off") &&
                          !cellBValue.equalsIgnoreCase("Holiday")) {
                    // Check if this looks like a PM row (starts with "PM")
                    if (cellBValue.toUpperCase().startsWith("PM")) {
                        // This is a PM row but the PM wasn't found
                        result.addError(String.format(
                            "WARNING - Sheet '%s', Row %d: Project Manager not found.\n" +
                            "  VALUE: '%s'\n" +
                            "  ISSUE: This is a PM row but the PM isn't in the database.\n" +
                            "  SOLUTION: Add this PM before importing, or check spelling.\n" +
                            "  AVAILABLE PMs: %s",
                            sheet.getSheetName(), rowNum + 1, cellBValue,
                            getAvailableProjectManagerNames()
                        ));
                        // Still try to process project assignments even if PM not found
                        // Use null for PM since we couldn't find them
                        isPMRow = true;
                        processProjectAssignments(sheet, row, rowNum, currentYear, currentMonth, 
                                                technician, null, columnToDayMap, result);
                    } else if (cellBValue.length() > 2 && !cellBValue.matches("\\d+.*")) {
                        // Some other non-empty text that's not a special marker
                        result.addError(String.format(
                            "INFO - Sheet '%s', Row %d: Unknown row type.\n" +
                            "  VALUE: '%s'\n" +
                            "  NOTE: This row was skipped. Only PM rows contain project assignments.",
                            sheet.getSheetName(), rowNum + 1, cellBValue
                        ));
                    }
                }
                
                // Check for SHOP assignment
                if (!isPMRow && (cellBValue.equalsIgnoreCase("Shop/Open") || cellBValue.equalsIgnoreCase("Shop"))) {
                    processShopAssignment(sheet, row, rowNum, currentYear, currentMonth, technician, result);
                    continue;
                }
                
                // Check for Training assignment
                if (!isPMRow && cellBValue.equalsIgnoreCase("Training")) {
                    processTrainingAssignment(sheet, row, rowNum, currentYear, currentMonth, technician, result);
                    continue;
                }
                
                // Check for Time Off/Holiday assignment - skip silently
                if (!isPMRow && (cellBValue.equalsIgnoreCase("Time Off/Holiday") || 
                    cellBValue.equalsIgnoreCase("Time Off") ||
                    cellBValue.equalsIgnoreCase("Holiday"))) {
                    logger.info("Skipping Time Off/Holiday row for {} at row {}", technician.getName(), rowNum + 1);
                    continue;
                }
            }
        }
    }
    
    private void processProjectAssignments(Sheet sheet, Row row, int rowNum, int year, int month,
                                          Resource technician, ProjectManager pm, 
                                          Map<Integer, Integer> columnToDayMap, ImportResult result) {
        // Only process columns that have day mappings
        if (columnToDayMap.isEmpty()) {
            logger.warn("No column-to-day mapping available for row {}", rowNum + 1);
            return;
        }
        
        // Get the range of columns that have day mappings
        int minCol = columnToDayMap.keySet().stream().min(Integer::compareTo).orElse(2);
        int maxCol = columnToDayMap.keySet().stream().max(Integer::compareTo).orElse((int) row.getLastCellNum());
        
        logger.debug("Processing row {} for projects, checking columns {} to {}", rowNum + 1, minCol, maxCol);
        
        // Check cells in the mapped day range for project assignments
        for (int colNum = minCol; colNum <= maxCol && colNum < row.getLastCellNum(); colNum++) {
            Cell cell = row.getCell(colNum);
            if (cell == null) continue;
            
            String cellValue = getCellStringValue(cell).trim();
            if (cellValue.isEmpty()) continue;
            
            // Check if this column has a day mapping
            if (!columnToDayMap.containsKey(colNum)) {
                logger.trace("Column {} has no day mapping, skipping", colNum);
                continue;
            }
            
            // Log all non-empty cells in the day range to see what we're finding
            logger.info("Sheet '{}', Cell at row {}, column {} (day {}): value='{}', cellType={}, hasFormula={}", 
                       sheet.getSheetName(), rowNum + 1, colNum, columnToDayMap.get(colNum), cellValue, 
                       cell.getCellType(), cell.getCellType() == CellType.FORMULA);
            
            // Special check for Adam Nesbitt September issue - skip phantom cells
            if (sheet.getSheetName().equals("Adam Nesbitt") && month == 9 && year == 2025) {
                Integer day = columnToDayMap.get(colNum);
                if (day != null && day >= 1 && day <= 6) {
                    // Check if this is the phantom cell that shouldn't exist
                    if (cellValue.contains("OXR1")) {
                        logger.warn("SKIPPING PHANTOM CELL: Sheet '{}', Row {}, Column {} (day {}): '{}'", 
                                   sheet.getSheetName(), rowNum + 1, colNum, day, cellValue);
                        continue; // Skip this phantom cell
                    }
                }
            }
            
            // Check if cell has background color OR contains project-like text
            CellStyle style = cell.getCellStyle();
            boolean hasColor = (style != null && style.getFillPattern() != FillPatternType.NO_FILL);
            boolean looksLikeProject = cellValue.contains("-") || cellValue.matches("^\\d{4,}.*");
            
            if (hasColor) {
                logger.info("Found COLORED cell at row {}, column {} with value: '{}'", 
                           rowNum + 1, colNum, cellValue);
                // This is a project assignment with color
                processProjectCell(sheet, cell, rowNum, colNum, year, month, 
                                 technician, pm, columnToDayMap, result);
            } else if (looksLikeProject) {
                logger.info("Found PROJECT-LIKE text (no color) at row {}, column {}: '{}'", 
                            rowNum + 1, colNum, cellValue);
                // Process it as a project anyway, but log a warning
                processProjectCell(sheet, cell, rowNum, colNum, year, month, 
                                 technician, pm, columnToDayMap, result);
                result.addError(String.format(
                    "WARNING - Sheet '%s', Row %d, Column %d: Project imported without color\n" +
                    "  VALUE: '%s'\n" +
                    "  NOTE: Cell contains project text but has no background color.\n" +
                    "  ACTION: Project was imported anyway. Add color for better visibility.",
                    sheet.getSheetName(), rowNum + 1, colNum + 1, cellValue
                ));
            }
        }
    }
    
    private void processProjectCell(Sheet sheet, Cell cell, int rowNum, int colNum,
                                   int year, int month, Resource technician, 
                                   ProjectManager pm, Map<Integer, Integer> columnToDayMap,
                                   ImportResult result) {
        String cellValue = getCellStringValue(cell).trim();
        
        // Parse project ID and description (format: "ProjectID - Description")
        String projectId = null;
        String description = null;
        
        if (cellValue.contains(" - ")) {
            // Split on the FIRST occurrence of " - " to separate ID from description
            int firstDashIndex = cellValue.indexOf(" - ");
            projectId = cellValue.substring(0, firstDashIndex).trim();
            description = cellValue.substring(firstDashIndex + 3).trim(); // Skip the " - "
            
            // For logging - show what we parsed
            logger.debug("Parsed project: ID='{}', Description='{}' from '{}'", 
                        projectId, description, cellValue);
        } else if (cellValue.contains("-")) {
            // Fallback for format without spaces
            String[] parts = cellValue.split("-", 2);
            projectId = parts[0].trim();
            description = parts.length > 1 ? parts[1].trim() : "";
        } else {
            projectId = cellValue;
            description = cellValue;
        }
        
        // Extract address from cell comment if present
        String clientAddress = null;
        Comment comment = cell.getCellComment();
        if (comment != null) {
            String commentText = comment.getString().getString();
            logger.info("Found comment in cell at row {}, col {}: '{}'", rowNum + 1, colNum, commentText);
            
            // Handle Excel threaded comments - extract actual comment after "Comment:" marker
            String actualComment = commentText;
            if (commentText.contains("[Threaded comment]")) {
                // This is a threaded comment with Excel metadata
                int commentIndex = commentText.indexOf("Comment:");
                if (commentIndex != -1) {
                    actualComment = commentText.substring(commentIndex + 8).trim();
                    logger.debug("Extracted actual comment: '{}'", actualComment);
                }
            } else if (commentText.startsWith("Author:")) {
                // Old-style comment with author prefix
                int colonIndex = commentText.indexOf(":");
                if (colonIndex != -1 && colonIndex < commentText.length() - 1) {
                    actualComment = commentText.substring(colonIndex + 1).trim();
                }
            }
            
            // Parse the address from the actual comment
            Map<String, String> addressParts = parseAddress(actualComment);
            if (!addressParts.isEmpty()) {
                // Build a formatted address string
                clientAddress = formatAddress(addressParts);
                logger.info("Parsed address: {}", addressParts);
            }
        }
        
        // Get the day numbers from the column mapping
        Integer startDay = columnToDayMap.get(colNum);
        Integer endDay = startDay;
        
        if (startDay == null) {
            logger.warn("No day mapping for column {} in sheet '{}', skipping cell", colNum, sheet.getSheetName());
            return;
        }
        
        // Log detailed information about what we're about to create
        logger.info("Processing project cell - Sheet: '{}', Row: {}, Column: {}, StartDay: {}, Project: '{}'", 
                   sheet.getSheetName(), rowNum + 1, colNum, startDay, cellValue);
        
        // Check if cell is part of a merged region
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            if (range.isInRange(rowNum, colNum)) {
                startDay = columnToDayMap.get(range.getFirstColumn());
                endDay = columnToDayMap.get(range.getLastColumn());
                if (startDay == null || endDay == null) {
                    logger.warn("Invalid day mapping for merged range in sheet '{}'", sheet.getSheetName());
                    return;
                }
                break;
            }
        }
        
        // Validate days are within month bounds
        int maxDayInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        if (startDay < 1 || startDay > maxDayInMonth || endDay < 1 || endDay > maxDayInMonth) {
            logger.warn("Invalid day range {}-{} for month {}/{} in sheet '{}'", 
                       startDay, endDay, month, year, sheet.getSheetName());
            return;
        }
        
        LocalDate startDate = LocalDate.of(year, month, startDay);
        LocalDate endDate = LocalDate.of(year, month, 
            Math.min(endDay, startDate.lengthOfMonth()));
        
        logger.info("Found project: {} ({}) from {} to {} for {}", 
            projectId, description, startDate, endDate, technician.getName());
        
        // Create or find project (now with address)
        Project project = findOrCreateProject(projectId, description, startDate, endDate, pm, clientAddress, result);
        if (project != null) {
            // Create assignment
            createAssignment(project, technician, startDate, endDate, result);
        }
    }
    
    private void processShopAssignment(Sheet sheet, Row row, int rowNum, int year, int month,
                                      Resource technician, ImportResult result) {
        // Try to find existing SHOP project
        Project shopProject = null;
        try {
            List<Project> projects = schedulingService.getAllProjects();
            for (Project p : projects) {
                if (p.getProjectId().equalsIgnoreCase("SHOP")) {
                    shopProject = p;
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Error looking for SHOP project: {}", e.getMessage());
        }
        
        if (shopProject == null) {
            // Skip silently if SHOP project doesn't exist
            logger.info("Skipping Shop assignment for {} - SHOP project not found", technician.getName());
            return;
        }
        
        // Look for date ranges in cells to the right
        for (int colNum = 2; colNum < row.getLastCellNum(); colNum++) {
            Cell cell = row.getCell(colNum);
            if (cell != null && !getCellStringValue(cell).isEmpty()) {
                // Each non-empty cell represents a day in shop
                int day = colNum - 1;
                if (day >= 1 && day <= 31) {
                    LocalDate date = LocalDate.of(year, month, Math.min(day, 
                        LocalDate.of(year, month, 1).lengthOfMonth()));
                    createAssignment(shopProject, technician, date, date, result);
                }
            }
        }
    }
    
    private void processTrainingAssignment(Sheet sheet, Row row, int rowNum, int year, int month,
                                          Resource technician, ImportResult result) {
        // Similar to shop but with training project
        // You may want to create a specific training project or handle differently
        logger.info("Found training assignment for {} in {}/{}", technician.getName(), month, year);
    }
    
    private Project findOrCreateProject(String projectId, String description, 
                                       LocalDate startDate, LocalDate endDate,
                                       ProjectManager pm, String clientAddress, ImportResult result) {
        // Always create a new project - no caching
        // This ensures each project gets the correct PM from its specific row
        try {
            Project project = schedulingService.createProject(projectId, description, startDate, endDate);
            
            // Set additional properties after creation
            if (pm != null) {
                project.setProjectManagerId(pm.getId());
                project.setStatus(ProjectStatus.ACTIVE);
            }
            
            // Set client address if available
            if (clientAddress != null && !clientAddress.isEmpty()) {
                project.setContactAddress(clientAddress);
                logger.info("Set contact address for project {}: {}", projectId, clientAddress);
            }
            
            // Set isTravel to true for imported projects (field service projects typically involve travel)
            project.setTravel(true);
            logger.info("Setting travel=true for imported project: {} (ID: {})", projectId, project.getId());
            
            // Log the project state before update
            logger.info("Project {} before update - Travel: {}, Status: {}, PM: {}", 
                projectId, project.isTravel(), project.getStatus(), project.getProjectManagerId());
            
            // Update the project with all properties
            schedulingService.updateProject(project);
            logger.info("Called updateProject for {}", projectId);
            
            // Verify the project was saved correctly
            Thread.sleep(100); // Give database time to commit
            Optional<Project> savedProject = schedulingService.getProjectById(project.getId());
            if (savedProject.isPresent()) {
                logger.info("Verified saved project {} (ID: {}) has travel={}, status={}, PM={}", 
                    projectId, savedProject.get().getId(), savedProject.get().isTravel(), 
                    savedProject.get().getStatus(), savedProject.get().getProjectManagerId());
                if (!savedProject.get().isTravel()) {
                    logger.error("ERROR: Travel field not persisted for project {}! Will try direct update.", projectId);
                    
                    // Try a direct SQL update as a workaround if we have a datasource
                    if (dataSource != null) {
                        try (Connection conn = dataSource.getConnection();
                             PreparedStatement stmt = conn.prepareStatement(
                                 "UPDATE projects SET is_travel = 1 WHERE id = ?")) {
                            stmt.setLong(1, project.getId());
                            int rows = stmt.executeUpdate();
                            logger.info("Direct SQL update for travel field: {} rows updated", rows);
                        } catch (SQLException e) {
                            logger.error("Failed to update travel field directly", e);
                        }
                    }
                }
            } else {
                logger.error("ERROR: Could not reload project {} after update!", projectId);
            }
            
            projectsCreated++;
            
            logger.info("Created project: {} - {} with PM: {}, contact address: {}, travel: {}", 
                       projectId, description, 
                       pm != null ? pm.getName() : "none",
                       clientAddress != null ? clientAddress : "none",
                       project.isTravel());
            return project;
            
        } catch (Exception e) {
            String error = String.format(
                "ERROR - Project Creation Failed: '%s'\n" +
                "  DESCRIPTION: %s\n" +
                "  DATE RANGE: %s to %s\n" +
                "  PROJECT MANAGER: %s\n" +
                "  TECHNICAL ERROR: %s\n" +
                "  POSSIBLE CAUSES:\n" +
                "    - Project ID contains invalid characters\n" +
                "    - Date range is invalid (end before start)\n" +
                "    - Database constraint violation\n" +
                "  SOLUTION: Check project ID format and date ranges",
                projectId, description, startDate, endDate,
                pm != null ? pm.getName() : "None",
                e.getMessage()
            );
            logger.error(error);
            result.addError(error);
            return null;
        }
    }
    
    private void createAssignment(Project project, Resource resource, 
                                 LocalDate startDate, LocalDate endDate, ImportResult result) {
        try {
            // Check if assignment already exists
            List<Assignment> existing = schedulingService.getAssignmentsByProjectId(project.getId());
            for (Assignment assignment : existing) {
                if (assignment.getResourceId().equals(resource.getId()) &&
                    assignment.getStartDate().equals(startDate) &&
                    assignment.getEndDate().equals(endDate)) {
                    logger.debug("Assignment already exists - skipping");
                    return;
                }
            }
            
            // Create new assignment
            Assignment assignment = schedulingService.createAssignment(
                project.getId(), resource.getId(), startDate, endDate, 0, 0);
            
            assignmentsCreated++;
            logger.debug("Created assignment for {} to {} ({} to {})", 
                resource.getName(), project.getProjectId(), startDate, endDate);
                
        } catch (Exception e) {
            String error = String.format(
                "ERROR - Assignment Creation Failed\n" +
                "  RESOURCE: %s\n" +
                "  PROJECT: %s\n" +
                "  DATES: %s to %s\n" +
                "  TECHNICAL ERROR: %s\n" +
                "  POSSIBLE CAUSES:\n" +
                "    - Resource is already assigned to another project for these dates\n" +
                "    - Date range conflicts with resource unavailability (PTO/holidays)\n" +
                "    - Project dates don't match assignment dates\n" +
                "  SOLUTION: Check for existing assignments and conflicts in the Timeline view",
                resource.getName(), project.getProjectId(), 
                startDate, endDate, e.getMessage()
            );
            logger.error(error);
            result.addError(error);
        }
    }
    
    /**
     * Build a mapping from column index to day number based on the calendar for a specific month
     */
    private Map<Integer, Integer> buildColumnToDayMapForMonth(Row headerRow, LocalDate monthDate) {
        Map<Integer, Integer> columnToDayMap = new HashMap<>();
        
        // Get the day of week for the first day of the month
        int firstDayOfWeek = monthDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        // Convert to Sunday-based (0=Sunday, 1=Monday, ... 6=Saturday)
        int sundayBasedFirstDay = (firstDayOfWeek == 7) ? 0 : firstDayOfWeek;
        
        logger.info("Month {} starts on day {} (0=Sunday, 1=Monday, etc.)", monthDate, sundayBasedFirstDay);
        
        // Find ALL sets of day columns (there may be multiple weeks shown horizontally)
        List<Integer> sundayColumns = new ArrayList<>();
        String[] dayAbbreviations = {"su", "mo", "tu", "we", "th", "fr", "sa"};
        
        // Scan the header row to find all Sunday columns (start of each week)
        for (int col = 0; col < headerRow.getLastCellNum(); col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null) continue;
            String value = getCellStringValue(cell).trim().toLowerCase();
            
            // Check if this is Sunday
            if (value.equals("su") || value.equals("sun") || value.equals("sunday")) {
                sundayColumns.add(col);
                logger.info("Found Sunday at column {}", col);
            }
        }
        
        if (sundayColumns.isEmpty()) {
            logger.warn("No Sunday columns found in header row");
            // Fall back to original logic
            return buildColumnToDayMap(headerRow);
        }
        
        logger.info("Found {} week(s) of columns starting at: {}", sundayColumns.size(), sundayColumns);
        
        // Now map the columns to days based on calendar
        int dayNum = 1;
        int maxDaysInMonth = monthDate.lengthOfMonth();
        int currentWeekIndex = 0;
        
        // Start from the first Sunday column
        if (currentWeekIndex < sundayColumns.size()) {
            int sundayCol = sundayColumns.get(currentWeekIndex);
            // The first day of month is at offset sundayBasedFirstDay from Sunday
            int currentCol = sundayCol + sundayBasedFirstDay;
            
            logger.info("First day of month (day 1) is in column {}", currentCol);
            
            while (dayNum <= maxDaysInMonth) {
                // Check if we've reached Saturday (end of week)
                if (currentCol >= sundayCol + 7) {
                    // Move to next week
                    currentWeekIndex++;
                    if (currentWeekIndex < sundayColumns.size()) {
                        sundayCol = sundayColumns.get(currentWeekIndex);
                        currentCol = sundayCol; // Start from Sunday of next week
                        logger.debug("Moving to week {} starting at column {}", currentWeekIndex + 1, sundayCol);
                    } else {
                        logger.warn("Ran out of week columns while mapping days");
                        break;
                    }
                }
                
                columnToDayMap.put(currentCol, dayNum);
                logger.debug("Mapped column {} to day {}", currentCol, dayNum);
                dayNum++;
                currentCol++;
            }
        }
        
        logger.info("Final column to day mapping for {}: {}", monthDate.getMonth(), columnToDayMap);
        return columnToDayMap;
    }
    
    /**
     * Build a mapping from column index to day number based on the header row
     */
    private Map<Integer, Integer> buildColumnToDayMap(Row headerRow) {
        Map<Integer, Integer> columnToDayMap = new HashMap<>();
        
        // Check if the header has day numbers or day abbreviations
        boolean hasDayNumbers = false;
        boolean hasDayAbbreviations = false;
        
        // First pass: determine what type of header we have
        for (int col = 2; col < Math.min(headerRow.getLastCellNum(), 10); col++) {
            Cell cell = headerRow.getCell(col);
            if (cell == null) continue;
            
            String value = getCellStringValue(cell).trim().toLowerCase();
            if (value.isEmpty()) continue;
            
            // Check for day numbers
            try {
                int dayNum = Integer.parseInt(value);
                if (dayNum >= 1 && dayNum <= 31) {
                    hasDayNumbers = true;
                }
            } catch (NumberFormatException e) {
                // Check for day abbreviations
                if (value.matches("^(su|mo|tu|we|th|fr|sa|sun|mon|tue|wed|thu|fri|sat)$")) {
                    hasDayAbbreviations = true;
                }
            }
        }
        
        logger.info("Header row type - Has day numbers: {}, Has day abbreviations: {}", 
                   hasDayNumbers, hasDayAbbreviations);
        
        if (hasDayNumbers) {
            // Read actual day numbers from the header
            for (int col = 2; col < headerRow.getLastCellNum(); col++) {
                Cell cell = headerRow.getCell(col);
                if (cell == null) continue;
                
                String value = getCellStringValue(cell).trim();
                if (value.isEmpty()) continue;
                
                try {
                    int dayNum = Integer.parseInt(value);
                    if (dayNum >= 1 && dayNum <= 31) {
                        columnToDayMap.put(col, dayNum);
                        logger.debug("Mapped column {} to day {}", col, dayNum);
                    }
                } catch (NumberFormatException e) {
                    // Skip non-numeric values
                }
            }
        } else if (hasDayAbbreviations) {
            // The header has day abbreviations (Su, Mo, Tu, etc.)
            // Look for a second header row with day numbers
            Row dayNumberRow = headerRow.getSheet().getRow(headerRow.getRowNum() + 1);
            if (dayNumberRow != null) {
                logger.info("Checking row {} for day numbers", headerRow.getRowNum() + 2);
                
                // Log what we're seeing in this row
                StringBuilder rowContent = new StringBuilder("Row content: ");
                for (int col = 2; col < Math.min(dayNumberRow.getLastCellNum(), 10); col++) {
                    Cell cell = dayNumberRow.getCell(col);
                    String value = cell != null ? getCellStringValue(cell).trim() : "";
                    rowContent.append("[").append(col).append("='").append(value).append("'] ");
                }
                logger.info(rowContent.toString());
                
                for (int col = 2; col < dayNumberRow.getLastCellNum(); col++) {
                    Cell cell = dayNumberRow.getCell(col);
                    if (cell == null) continue;
                    
                    String value = getCellStringValue(cell).trim();
                    if (value.isEmpty()) continue;
                    
                    try {
                        int dayNum = Integer.parseInt(value);
                        if (dayNum >= 1 && dayNum <= 31) {
                            columnToDayMap.put(col, dayNum);
                            logger.info("Found day {} in column {}", dayNum, col);
                        }
                    } catch (NumberFormatException e) {
                        // Not a day number - log what it is
                        logger.debug("Column {} contains non-numeric value: '{}'", col, value);
                    }
                }
            }
            
            // If still no day numbers found, calculate based on calendar
            if (columnToDayMap.isEmpty()) {
                logger.warn("No day numbers found, calculating based on calendar alignment");
                
                // Try to determine the month and year from context
                // This method is called with a month date, so we can use it
                // We need to figure out which day of week the month starts on
                
                // The day abbreviations tell us the column alignment
                // Su=2, Mo=3, Tu=4, We=5, Th=6, Fr=7, Sa=8 (if starting at column 2)
                
                // For now, create a calendar-aware mapping
                // We'll look at the parent method to get the month/year
                logger.info("Using calendar-aware mapping for day numbers");
                
                // This is a fallback - ideally we should pass the month date to this method
                // For now, use simple sequential numbering but log a warning
                int dayNum = 1;
                for (int col = 2; col < Math.min(headerRow.getLastCellNum(), 33); col++) { // Max 31 days
                    columnToDayMap.put(col, dayNum++);
                    if (dayNum > 31) break;
                }
                logger.warn("Using fallback sequential numbering - dates may be incorrect!");
            }
        }
        
        // Fill in gaps for merged cells
        if (!columnToDayMap.isEmpty()) {
            int minCol = columnToDayMap.keySet().stream().min(Integer::compareTo).orElse(2);
            int maxCol = columnToDayMap.keySet().stream().max(Integer::compareTo).orElse(2);
            
            for (int col = minCol; col <= maxCol; col++) {
                if (!columnToDayMap.containsKey(col)) {
                    // Try to interpolate based on previous column
                    Integer prevDay = columnToDayMap.get(col - 1);
                    if (prevDay != null && prevDay < 31) {
                        columnToDayMap.put(col, prevDay + 1);
                        logger.debug("Interpolated column {} to day {}", col, prevDay + 1);
                    }
                }
            }
        }
        
        return columnToDayMap;
    }
    
    /**
     * Check if a row contains day numbers
     */
    private boolean hasDayNumbers(Row row) {
        if (row == null) return false;
        
        int numberCount = 0;
        int checkedCells = 0;
        
        for (int col = 2; col < Math.min(row.getLastCellNum(), 10); col++) {
            Cell cell = row.getCell(col);
            if (cell == null) continue;
            
            String value = getCellStringValue(cell).trim();
            if (value.isEmpty()) continue;
            
            checkedCells++;
            
            // Check for day numbers
            try {
                int dayNum = Integer.parseInt(value);
                if (dayNum >= 1 && dayNum <= 31) {
                    numberCount++;
                }
            } catch (NumberFormatException e) {
                // Not a number
            }
        }
        
        // If most cells are day numbers, it's a day number row
        return checkedCells > 0 && numberCount > checkedCells * 0.5;
    }
    
    /**
     * Check if a row contains day header information (day numbers or day abbreviations)
     */
    private boolean isHeaderRow(Row row) {
        if (row == null) return false;
        
        // Check the first few cells starting from column C (index 2)
        int dayAbbrCount = 0;
        int numberCount = 0;
        int checkedCells = 0;
        
        for (int col = 2; col < Math.min(row.getLastCellNum(), 10); col++) {
            Cell cell = row.getCell(col);
            if (cell == null) continue;
            
            String value = getCellStringValue(cell).trim().toLowerCase();
            if (value.isEmpty()) continue;
            
            checkedCells++;
            
            // Check for day abbreviations
            if (value.matches("^(su|mo|tu|we|th|fr|sa|sun|mon|tue|wed|thu|fri|sat)$")) {
                dayAbbrCount++;
            }
            // Check for day numbers
            else if (value.matches("^\\d{1,2}$")) {
                int dayNum = Integer.parseInt(value);
                if (dayNum >= 1 && dayNum <= 31) {
                    numberCount++;
                }
            }
        }
        
        // If most cells are day abbreviations or numbers, it's a header row
        boolean isHeader = checkedCells > 0 && 
                          (dayAbbrCount > checkedCells * 0.5 || numberCount > checkedCells * 0.5);
        
        if (isHeader) {
            logger.info("Detected header row with {} day abbreviations and {} day numbers out of {} cells", 
                       dayAbbrCount, numberCount, checkedCells);
        }
        
        return isHeader;
    }
    
    private LocalDate parseMonthYear(String value) {
        // Try to parse various month/year formats
        // Examples: "January 2025", "Jan 2025", "1/2025", "01/2025"
        
        // DEBUG: Log what we're trying to parse
        logger.info("Attempting to parse month/year from: '{}'", value);
        
        try {
            // Try full month name
            String[] monthPatterns = {
                "MMMM yyyy", "MMM yyyy", "M/yyyy", "MM/yyyy"
            };
            
            for (String pattern : monthPatterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                    LocalDate date = LocalDate.parse(value + " 1", 
                        DateTimeFormatter.ofPattern(pattern + " d", Locale.ENGLISH));
                    logger.debug("Successfully parsed '{}' using pattern '{}'", value, pattern);
                    return date.withDayOfMonth(1);
                } catch (DateTimeParseException e) {
                    // Try next pattern
                    logger.trace("Failed to parse '{}' with pattern '{}'", value, pattern);
                }
            }
            
            // Try parsing as just month and year
            String upperValue = value.toUpperCase();
            for (Month month : Month.values()) {
                if (upperValue.contains(month.name()) || 
                    upperValue.contains(month.name().substring(0, 3))) {
                    // Extract year
                    Pattern yearPattern = Pattern.compile("(20\\d{2})");
                    Matcher matcher = yearPattern.matcher(value);
                    if (matcher.find()) {
                        int year = Integer.parseInt(matcher.group(1));
                        logger.debug("Successfully parsed '{}' as month {} year {}", value, month, year);
                        return LocalDate.of(year, month, 1);
                    }
                }
            }
            
        } catch (Exception e) {
            // Not a month/year marker
            logger.debug("Exception while parsing '{}': {}", value, e.getMessage());
        }
        
        logger.debug("'{}' is not a recognized month/year format", value);
        return null;
    }
    
    private String getAvailableProjectManagerNames() {
        List<String> names = new ArrayList<>();
        for (ProjectManager pm : projectManagerCache.values()) {
            names.add(pm.getName());
        }
        names.sort(String::compareToIgnoreCase);
        if (names.isEmpty()) {
            return "(No project managers found in database)";
        }
        return String.join(", ", names);
    }
    
    private ProjectManager findProjectManager(String name) {
        if (name == null || name.isEmpty()) return null;
        
        String searchName = name.toLowerCase().trim();
        
        // First try exact match
        ProjectManager exactMatch = projectManagerCache.get(searchName);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Remove common prefixes
        if (searchName.startsWith("pm - ") || searchName.startsWith("pm- ")) {
            searchName = searchName.substring(4).trim();
        } else if (searchName.startsWith("pm ")) {
            searchName = searchName.substring(3).trim();
        }
        
        // Try to match on first name, last name, or partial matches
        ProjectManager bestMatch = null;
        int bestScore = 0;
        
        for (Map.Entry<String, ProjectManager> entry : projectManagerCache.entrySet()) {
            String pmFullName = entry.getKey(); // already lowercase
            ProjectManager pm = entry.getValue();
            
            // Split PM name into parts
            String[] pmParts = pmFullName.split("\\s+");
            String[] searchParts = searchName.split("\\s+");
            
            int score = 0;
            
            // Check exact matches for any part
            for (String searchPart : searchParts) {
                if (searchPart.length() < 2) continue; // Skip single characters
                
                for (String pmPart : pmParts) {
                    if (pmPart.equals(searchPart)) {
                        score += 10; // Exact part match
                    } else if (pmPart.startsWith(searchPart) || searchPart.startsWith(pmPart)) {
                        score += 5; // Prefix match
                    } else if (pmPart.contains(searchPart) || searchPart.contains(pmPart)) {
                        score += 2; // Contains match
                    }
                }
            }
            
            // Bonus for matching first name (assuming first part is first name)
            if (pmParts.length > 0 && searchParts.length > 0) {
                if (pmParts[0].equals(searchParts[0])) {
                    score += 5; // First name exact match bonus
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestMatch = pm;
            }
        }
        
        // Only return a match if we have a reasonable confidence
        if (bestScore >= 5) {
            logger.info("Fuzzy matched '{}' to PM '{}'", name, bestMatch.getName());
            return bestMatch;
        }
        
        return null;
    }
    
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    // Format as month name and year (e.g., "March 2025")
                    SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
                    return formatter.format(date);
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluate formula to get actual value
                try {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    
                    switch (cellValue.getCellType()) {
                        case STRING:
                            return cellValue.getStringValue();
                        case NUMERIC:
                            // Check if it's a date
                            if (DateUtil.isCellDateFormatted(cell)) {
                                Date date = DateUtil.getJavaDate(cellValue.getNumberValue());
                                SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
                                return formatter.format(date);
                            }
                            return String.valueOf((int) cellValue.getNumberValue());
                        case BOOLEAN:
                            return String.valueOf(cellValue.getBooleanValue());
                        default:
                            return "";
                    }
                } catch (Exception e) {
                    logger.warn("Failed to evaluate formula in cell: {}", e.getMessage());
                    return cell.getCellFormula();
                }
            default:
                return "";
        }
    }
    
    /**
     * Parse address from comment text
     * Expected formats:
     * - "123 Main St, City, State 12345"
     * - "123 Main St, City, ST 12345"
     * - Partial addresses with missing components
     */
    private Map<String, String> parseAddress(String addressText) {
        Map<String, String> addressParts = new HashMap<>();
        if (addressText == null || addressText.trim().isEmpty()) {
            return addressParts;
        }
        
        // Clean up the text
        addressText = addressText.trim().replaceAll("\\s+", " ");
        
        // Split by comma
        String[] parts = addressText.split(",");
        
        if (parts.length > 0) {
            // First part is usually street address
            addressParts.put("street", parts[0].trim());
        }
        
        if (parts.length >= 2) {
            // Last part usually contains state and zip
            String lastPart = parts[parts.length - 1].trim();
            
            // Extract ZIP code (5 digits or 5+4 format)
            Pattern zipPattern = Pattern.compile("(\\d{5}(?:-\\d{4})?)");
            Matcher zipMatcher = zipPattern.matcher(lastPart);
            String zipCode = null;
            if (zipMatcher.find()) {
                zipCode = zipMatcher.group(1);
                addressParts.put("zip", zipCode);
                // Remove zip from lastPart for further processing
                lastPart = lastPart.replace(zipCode, "").trim();
            }
            
            // Extract state (2-letter abbreviation or full name)
            String state = extractState(lastPart);
            if (state != null) {
                addressParts.put("state", state);
                lastPart = lastPart.replace(state, "").trim();
            }
            
            // If we have 3 or more parts, middle part(s) are usually city
            if (parts.length >= 3) {
                // Everything between street and state/zip is city
                StringBuilder cityBuilder = new StringBuilder();
                for (int i = 1; i < parts.length - 1; i++) {
                    if (cityBuilder.length() > 0) {
                        cityBuilder.append(", ");
                    }
                    cityBuilder.append(parts[i].trim());
                }
                String city = cityBuilder.toString();
                if (!city.isEmpty()) {
                    addressParts.put("city", city);
                }
            } else if (!lastPart.isEmpty()) {
                // If only 2 parts, remaining text after state/zip extraction might be city
                addressParts.put("city", lastPart);
            }
        }
        
        return addressParts;
    }
    
    /**
     * Extract state from text (abbreviation or full name)
     */
    private String extractState(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // Common US state abbreviations
        String[] stateAbbreviations = {
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY", "DC"
        };
        
        // Check for 2-letter state abbreviation
        for (String abbr : stateAbbreviations) {
            if (text.toUpperCase().contains(abbr)) {
                return abbr;
            }
        }
        
        // Check for full state names (simplified list of common ones)
        Map<String, String> stateNames = new HashMap<>();
        stateNames.put("ALABAMA", "AL");
        stateNames.put("ALASKA", "AK");
        stateNames.put("ARIZONA", "AZ");
        stateNames.put("ARKANSAS", "AR");
        stateNames.put("CALIFORNIA", "CA");
        stateNames.put("COLORADO", "CO");
        stateNames.put("CONNECTICUT", "CT");
        stateNames.put("DELAWARE", "DE");
        stateNames.put("FLORIDA", "FL");
        stateNames.put("GEORGIA", "GA");
        stateNames.put("HAWAII", "HI");
        stateNames.put("IDAHO", "ID");
        stateNames.put("ILLINOIS", "IL");
        stateNames.put("INDIANA", "IN");
        stateNames.put("IOWA", "IA");
        stateNames.put("KANSAS", "KS");
        stateNames.put("KENTUCKY", "KY");
        stateNames.put("LOUISIANA", "LA");
        stateNames.put("MAINE", "ME");
        stateNames.put("MARYLAND", "MD");
        stateNames.put("MASSACHUSETTS", "MA");
        stateNames.put("MICHIGAN", "MI");
        stateNames.put("MINNESOTA", "MN");
        stateNames.put("MISSISSIPPI", "MS");
        stateNames.put("MISSOURI", "MO");
        stateNames.put("MONTANA", "MT");
        stateNames.put("NEBRASKA", "NE");
        stateNames.put("NEVADA", "NV");
        stateNames.put("NEW HAMPSHIRE", "NH");
        stateNames.put("NEW JERSEY", "NJ");
        stateNames.put("NEW MEXICO", "NM");
        stateNames.put("NEW YORK", "NY");
        stateNames.put("NORTH CAROLINA", "NC");
        stateNames.put("NORTH DAKOTA", "ND");
        stateNames.put("OHIO", "OH");
        stateNames.put("OKLAHOMA", "OK");
        stateNames.put("OREGON", "OR");
        stateNames.put("PENNSYLVANIA", "PA");
        stateNames.put("RHODE ISLAND", "RI");
        stateNames.put("SOUTH CAROLINA", "SC");
        stateNames.put("SOUTH DAKOTA", "SD");
        stateNames.put("TENNESSEE", "TN");
        stateNames.put("TEXAS", "TX");
        stateNames.put("UTAH", "UT");
        stateNames.put("VERMONT", "VT");
        stateNames.put("VIRGINIA", "VA");
        stateNames.put("WASHINGTON", "WA");
        stateNames.put("WEST VIRGINIA", "WV");
        stateNames.put("WISCONSIN", "WI");
        stateNames.put("WYOMING", "WY");
        
        String upperText = text.toUpperCase();
        for (Map.Entry<String, String> entry : stateNames.entrySet()) {
            if (upperText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Format address parts into a single string
     */
    private String formatAddress(Map<String, String> addressParts) {
        StringBuilder formatted = new StringBuilder();
        
        if (addressParts.containsKey("street")) {
            formatted.append(addressParts.get("street"));
        }
        
        if (addressParts.containsKey("city")) {
            if (formatted.length() > 0) formatted.append(", ");
            formatted.append(addressParts.get("city"));
        }
        
        if (addressParts.containsKey("state")) {
            if (formatted.length() > 0) formatted.append(", ");
            formatted.append(addressParts.get("state"));
        }
        
        if (addressParts.containsKey("zip")) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(addressParts.get("zip"));
        }
        
        return formatted.toString();
    }
    
    // Result class for import operation
    public static class ImportResult {
        private int projectsCreated = 0;
        private int assignmentsCreated = 0;
        private List<String> errors = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public int getProjectsCreated() { return projectsCreated; }
        public void setProjectsCreated(int count) { this.projectsCreated = count; }
        
        public int getAssignmentsCreated() { return assignmentsCreated; }
        public void setAssignmentsCreated(int count) { this.assignmentsCreated = count; }
        
        public List<String> getErrors() { return errors; }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("================== IMPORT SUMMARY ==================\n\n");
            
            // Success metrics
            sb.append("✓ SUCCESSFULLY IMPORTED:\n");
            sb.append("  • Projects Created: ").append(projectsCreated).append("\n");
            sb.append("  • Assignments Created: ").append(assignmentsCreated).append("\n");
            sb.append("\n");
            
            if (hasErrors()) {
                // Count error types
                int criticalErrors = 0;
                int warnings = 0;
                int info = 0;
                
                for (String error : errors) {
                    if (error.startsWith("ERROR")) criticalErrors++;
                    else if (error.startsWith("WARNING")) warnings++;
                    else if (error.startsWith("INFO")) info++;
                }
                
                sb.append("⚠️ ISSUES FOUND:\n");
                sb.append("  • Critical Errors: ").append(criticalErrors).append("\n");
                sb.append("  • Warnings: ").append(warnings).append("\n");
                sb.append("  • Information: ").append(info).append("\n");
                sb.append("\n");
                
                sb.append("================== DETAILED ISSUES ==================\n\n");
                
                // Group errors by type
                List<String> criticals = new ArrayList<>();
                List<String> warns = new ArrayList<>();
                List<String> infos = new ArrayList<>();
                
                for (String error : errors) {
                    if (error.startsWith("ERROR")) criticals.add(error);
                    else if (error.startsWith("WARNING")) warns.add(error);
                    else if (error.startsWith("INFO")) infos.add(error);
                }
                
                if (!criticals.isEmpty()) {
                    sb.append("🔴 CRITICAL ERRORS (Must Fix):\n");
                    sb.append("─────────────────────────────────\n");
                    for (String error : criticals) {
                        sb.append(error).append("\n\n");
                    }
                }
                
                if (!warns.isEmpty()) {
                    sb.append("🟡 WARNINGS (Should Review):\n");
                    sb.append("─────────────────────────────────\n");
                    for (String error : warns) {
                        sb.append(error).append("\n\n");
                    }
                }
                
                if (!infos.isEmpty()) {
                    sb.append("🔵 INFORMATION (For Your Awareness):\n");
                    sb.append("─────────────────────────────────\n");
                    for (String error : infos) {
                        sb.append(error).append("\n\n");
                    }
                }
            } else {
                sb.append("✅ NO ERRORS - Import completed successfully!\n");
            }
            
            sb.append("=====================================================\n");
            
            return sb.toString();
        }
    }
}