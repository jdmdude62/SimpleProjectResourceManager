package com.subliminalsearch.simpleprojectresourcemanager.util;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class DebugTravelUpdate {
    private static final Logger logger = Logger.getLogger(DebugTravelUpdate.class.getName());
    
    public static void main(String[] args) throws Exception {
        // Initialize database
        DatabaseConfig dbConfig = new DatabaseConfig();
        HikariDataSource dataSource = dbConfig.getDataSource();
        
        // Create a test project with travel = false
        Project testProject = new Project("DEBUG-TRAVEL", "Debug Travel Test", 
            LocalDate.now(), LocalDate.now().plusDays(30));
        testProject.setStatus(ProjectStatus.ACTIVE);
        testProject.setTravel(false);
        testProject.setContactName("Test Contact");
        testProject.setContactEmail("test@example.com");
        testProject.setContactPhone("555-1234");
        testProject.setContactCompany("Test Company");
        testProject.setContactRole("Manager");
        testProject.setSendReports(false);
        testProject.setReportFrequency("Monthly");
        
        System.out.println("=== DEBUG TRAVEL UPDATE ===");
        System.out.println("Creating test project with travel = false");
        
        // Insert the project first
        String insertSql = """
            INSERT INTO projects (project_id, description, start_date, end_date, status,
                contact_name, contact_email, contact_phone, contact_company, contact_role,
                send_reports, report_frequency, is_travel, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        long projectId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setString(1, testProject.getProjectId());
            stmt.setString(2, testProject.getDescription());
            stmt.setString(3, testProject.getStartDate().toString());
            stmt.setString(4, testProject.getEndDate().toString());
            stmt.setString(5, testProject.getStatus().name());
            stmt.setString(6, testProject.getContactName());
            stmt.setString(7, testProject.getContactEmail());
            stmt.setString(8, testProject.getContactPhone());
            stmt.setString(9, testProject.getContactCompany());
            stmt.setString(10, testProject.getContactRole());
            stmt.setInt(11, testProject.isSendReports() ? 1 : 0);
            stmt.setString(12, testProject.getReportFrequency());
            stmt.setInt(13, testProject.isTravel() ? 1 : 0);
            stmt.setTimestamp(14, Timestamp.valueOf(now));
            stmt.setTimestamp(15, Timestamp.valueOf(now));
            
            int affectedRows = stmt.executeUpdate();
            System.out.println("Inserted project: " + affectedRows + " rows affected");
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    projectId = generatedKeys.getLong(1);
                    testProject.setId(projectId);
                    System.out.println("Generated project ID: " + projectId);
                } else {
                    throw new SQLException("Failed to get generated ID");
                }
            }
        }
        
        // Verify the inserted value
        String selectSql = "SELECT is_travel FROM projects WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int travelValue = rs.getInt("is_travel");
                    System.out.println("After INSERT - is_travel in database: " + travelValue);
                }
            }
        }
        
        // Now test the UPDATE with travel = true
        System.out.println("\nTesting UPDATE with travel = true");
        testProject.setTravel(true);
        
        // Check if contact_address column exists
        boolean hasContactAddress = false;
        try (Connection testConn = dataSource.getConnection()) {
            DatabaseMetaData meta = testConn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "projects", "contact_address")) {
                hasContactAddress = rs.next();
            }
        }
        
        System.out.println("Has contact_address column: " + hasContactAddress);
        
        // Replicate the exact UPDATE logic from ProjectRepository
        final boolean useContactAddress = hasContactAddress;
        String sql;
        if (useContactAddress) {
            sql = """
                UPDATE projects 
                SET project_id = ?, description = ?, project_manager_id = ?, start_date = ?, end_date = ?, status = ?,
                    contact_name = ?, contact_email = ?, contact_phone = ?, contact_company = ?, contact_role = ?,
                    contact_address = ?, send_reports = ?, report_frequency = ?, is_travel = ?, updated_at = ?
                WHERE id = ?
                """;
        } else {
            sql = """
                UPDATE projects 
                SET project_id = ?, description = ?, project_manager_id = ?, start_date = ?, end_date = ?, status = ?,
                    contact_name = ?, contact_email = ?, contact_phone = ?, contact_company = ?, contact_role = ?,
                    send_reports = ?, report_frequency = ?, is_travel = ?, updated_at = ?
                WHERE id = ?
                """;
        }
        
        System.out.println("Using SQL: " + sql);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            testProject.setUpdatedAt(LocalDateTime.now());
            
            stmt.setString(1, testProject.getProjectId());
            stmt.setString(2, testProject.getDescription());
            if (testProject.getProjectManagerId() != null) {
                stmt.setLong(3, testProject.getProjectManagerId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, testProject.getStartDate().toString());
            stmt.setString(5, testProject.getEndDate().toString());
            stmt.setString(6, testProject.getStatus().name());
            
            // Client contact fields
            int paramIndex = 7;
            System.out.println("Setting contact parameters starting at index " + paramIndex);
            stmt.setString(paramIndex++, testProject.getContactName());
            stmt.setString(paramIndex++, testProject.getContactEmail());
            stmt.setString(paramIndex++, testProject.getContactPhone());
            stmt.setString(paramIndex++, testProject.getContactCompany());
            stmt.setString(paramIndex++, testProject.getContactRole());
            
            if (useContactAddress) {
                System.out.println("Setting contact_address at index " + paramIndex);
                stmt.setString(paramIndex++, testProject.getContactAddress());
            }
            
            System.out.println("Setting send_reports at index " + paramIndex);
            stmt.setInt(paramIndex++, testProject.isSendReports() ? 1 : 0);
            System.out.println("Setting report_frequency at index " + paramIndex);
            stmt.setString(paramIndex++, testProject.getReportFrequency());
            
            int travelValue = testProject.isTravel() ? 1 : 0;
            System.out.println("Setting travel field to " + travelValue + " for project " + testProject.getProjectId() + 
                             " at parameter index " + paramIndex);
            stmt.setInt(paramIndex++, travelValue);
            
            System.out.println("Setting updated_at at index " + paramIndex);
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(testProject.getUpdatedAt()));
            System.out.println("Setting id WHERE clause at index " + paramIndex);
            stmt.setLong(paramIndex++, testProject.getId());
            
            System.out.println("Executing UPDATE...");
            int affectedRows = stmt.executeUpdate();
            System.out.println("Updated project: " + testProject.getProjectId() + " - " + affectedRows + " rows affected");
        }
        
        // Verify the updated value
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int travelValue = rs.getInt("is_travel");
                    System.out.println("After UPDATE - is_travel in database: " + travelValue);
                    System.out.println("Expected: 1, Actual: " + travelValue + 
                                     (travelValue == 1 ? " [CORRECT]" : " [INCORRECT]"));
                }
            }
        }
        
        // Test UPDATE with travel = false
        System.out.println("\nTesting UPDATE with travel = false");
        testProject.setTravel(false);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            testProject.setUpdatedAt(LocalDateTime.now());
            
            stmt.setString(1, testProject.getProjectId());
            stmt.setString(2, testProject.getDescription());
            if (testProject.getProjectManagerId() != null) {
                stmt.setLong(3, testProject.getProjectManagerId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, testProject.getStartDate().toString());
            stmt.setString(5, testProject.getEndDate().toString());
            stmt.setString(6, testProject.getStatus().name());
            
            // Client contact fields
            int paramIndex = 7;
            stmt.setString(paramIndex++, testProject.getContactName());
            stmt.setString(paramIndex++, testProject.getContactEmail());
            stmt.setString(paramIndex++, testProject.getContactPhone());
            stmt.setString(paramIndex++, testProject.getContactCompany());
            stmt.setString(paramIndex++, testProject.getContactRole());
            
            if (useContactAddress) {
                stmt.setString(paramIndex++, testProject.getContactAddress());
            }
            
            stmt.setInt(paramIndex++, testProject.isSendReports() ? 1 : 0);
            stmt.setString(paramIndex++, testProject.getReportFrequency());
            
            int travelValue = testProject.isTravel() ? 1 : 0;
            System.out.println("Setting travel field to " + travelValue + " for project " + testProject.getProjectId() + 
                             " at parameter index " + paramIndex);
            stmt.setInt(paramIndex++, travelValue);
            
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(testProject.getUpdatedAt()));
            stmt.setLong(paramIndex++, testProject.getId());
            
            System.out.println("Executing UPDATE...");
            int affectedRows = stmt.executeUpdate();
            System.out.println("Updated project: " + testProject.getProjectId() + " - " + affectedRows + " rows affected");
        }
        
        // Verify the final value
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            
            stmt.setLong(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int travelValue = rs.getInt("is_travel");
                    System.out.println("After final UPDATE - is_travel in database: " + travelValue);
                    System.out.println("Expected: 0, Actual: " + travelValue + 
                                     (travelValue == 0 ? " [CORRECT]" : " [INCORRECT]"));
                }
            }
        }
        
        // Clean up
        String deleteSql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setLong(1, projectId);
            int deleted = stmt.executeUpdate();
            System.out.println("\nCleaned up test project: " + deleted + " rows deleted");
        }
        
        dataSource.close();
        System.out.println("\nDone.");
    }
}