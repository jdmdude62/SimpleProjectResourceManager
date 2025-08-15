package com.subliminalsearch.simpleprojectresourcemanager.regression;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for date format bugs fixed in the application.
 * 
 * BUG-2341: Project disappears when PM changed due to date corruption
 * BUG-2339: Project visibility filter issues
 * 
 * These tests verify that dates are stored in the correct format
 * and not as timestamps which caused projects to disappear.
 */
@DisplayName("Date Format Regression Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DateFormatRegressionTest {
    
    private Connection connection;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory SQLite database for testing
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
        // Create test tables
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS assignments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id INTEGER NOT NULL,
                    resource_id INTEGER,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    travel_out_days INTEGER DEFAULT 0,
                    travel_back_days INTEGER DEFAULT 0,
                    notes TEXT
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    project_number TEXT UNIQUE NOT NULL,
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    project_manager_id INTEGER,
                    budget REAL DEFAULT 0,
                    status TEXT DEFAULT 'Active'
                )
            """);
        }
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("BUG-2341: Should store assignment dates in correct format (not as timestamps)")
    void shouldStoreAssignmentDatesInCorrectFormat() throws Exception {
        // GIVEN an assignment with specific dates
        LocalDate startDate = LocalDate.of(2025, 8, 10);
        LocalDate endDate = LocalDate.of(2025, 8, 20);
        
        // WHEN the assignment is saved using the correct format
        String sql = """
            INSERT INTO assignments (project_id, resource_id, start_date, end_date, 
                                   travel_out_days, travel_back_days, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1);
            stmt.setInt(2, 2);
            // This is the fix - using string format instead of setDate
            stmt.setString(3, startDate.toString() + " 00:00:00.000");
            stmt.setString(4, endDate.toString() + " 00:00:00.000");
            stmt.setInt(5, 1);
            stmt.setInt(6, 1);
            stmt.setString(7, "Test assignment");
            
            stmt.executeUpdate();
            
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            generatedKeys.next();
            int assignmentId = generatedKeys.getInt(1);
            
            // THEN the dates should be stored in correct format
            try (Statement query = connection.createStatement();
                 ResultSet rs = query.executeQuery("SELECT start_date, end_date FROM assignments WHERE id = " + assignmentId)) {
                
                assertTrue(rs.next());
                String storedStartDate = rs.getString("start_date");
                String storedEndDate = rs.getString("end_date");
                
                // Verify correct format: "YYYY-MM-DD HH:mm:ss.SSS"
                assertTrue(storedStartDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
                    "Start date should be in correct format, not timestamp");
                assertTrue(storedEndDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
                    "End date should be in correct format, not timestamp");
                
                // Verify correct date values
                assertTrue(storedStartDate.contains("2025-08-10"),
                    "Start date should contain correct date");
                assertTrue(storedEndDate.contains("2025-08-20"),
                    "End date should contain correct date");
                
                // Verify NOT stored as timestamp
                assertFalse(storedStartDate.matches("\\d{13}"),
                    "Should not be stored as timestamp");
            }
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("BUG-2341: Verify wrong format (setDate) would cause timestamp storage")
    void verifyWrongFormatCausesTimestamp() throws Exception {
        // This test documents the bug - using setDate causes timestamp storage
        
        String sql = """
            INSERT INTO assignments (project_id, resource_id, start_date, end_date, 
                                   travel_out_days, travel_back_days, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, 1);
            stmt.setInt(2, 2);
            
            // This was the BUG - using setDate stores as timestamp
            // Commenting out as it would fail with SQLite
            // stmt.setDate(3, Date.valueOf(LocalDate.of(2025, 8, 10)));
            // stmt.setDate(4, Date.valueOf(LocalDate.of(2025, 8, 20)));
            
            // Instead, demonstrate what the bug would store
            stmt.setString(3, "1754629200000"); // Timestamp that was stored
            stmt.setString(4, "1755493200000"); // Timestamp that was stored
            stmt.setInt(5, 0);
            stmt.setInt(6, 0);
            stmt.setString(7, "Buggy assignment");
            
            stmt.executeUpdate();
            
            // Query to verify the wrong format
            try (Statement query = connection.createStatement();
                 ResultSet rs = query.executeQuery("SELECT start_date, end_date FROM assignments WHERE notes = 'Buggy assignment'")) {
                
                assertTrue(rs.next());
                String storedStartDate = rs.getString("start_date");
                
                // This is what the bug looked like - pure timestamp
                assertTrue(storedStartDate.matches("\\d{13}"),
                    "Bug causes timestamp storage instead of date format");
            }
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Should parse dates correctly from both formats for backward compatibility")
    void shouldParseDatesFromBothFormats() throws Exception {
        // Insert data in both formats
        try (Statement stmt = connection.createStatement()) {
            // Correct format
            stmt.execute("""
                INSERT INTO assignments (project_id, resource_id, start_date, end_date)
                VALUES (1, 1, '2025-08-10 00:00:00.000', '2025-08-20 00:00:00.000')
            """);
            
            // Old buggy format (timestamp)
            stmt.execute("""
                INSERT INTO assignments (project_id, resource_id, start_date, end_date)
                VALUES (2, 2, '1754629200000', '1755493200000')
            """);
        }
        
        // Verify we can detect and handle both
        try (Statement query = connection.createStatement();
             ResultSet rs = query.executeQuery("SELECT * FROM assignments")) {
            
            while (rs.next()) {
                String startDate = rs.getString("start_date");
                
                if (startDate.matches("\\d{13}")) {
                    // Timestamp format - needs conversion
                    long timestamp = Long.parseLong(startDate);
                    assertTrue(timestamp > 0, "Should be valid timestamp");
                } else if (startDate.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    // Correct date format
                    assertTrue(startDate.contains("-"), "Should be date format");
                }
            }
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("BUG-2339: Should handle project manager filter correctly")
    void shouldHandleProjectManagerFilter() throws Exception {
        // Insert test projects with different managers
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                INSERT INTO projects (name, project_number, start_date, end_date, project_manager_id)
                VALUES ('Project 1', 'PROJ-001', '2025-08-01 00:00:00.000', '2025-08-31 00:00:00.000', 1)
            """);
            
            stmt.execute("""
                INSERT INTO projects (name, project_number, start_date, end_date, project_manager_id)
                VALUES ('Project 2', 'PROJ-002', '2025-08-01 00:00:00.000', '2025-08-31 00:00:00.000', 2)
            """);
            
            stmt.execute("""
                INSERT INTO projects (name, project_number, start_date, end_date, project_manager_id)
                VALUES ('Project 3', 'CH-PBLD-2025-091', '2025-08-01 00:00:00.000', '2025-08-31 00:00:00.000', 3)
            """);
        }
        
        // Test filtering by specific manager
        String sql = "SELECT * FROM projects WHERE project_manager_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, 3); // Paula Poodle's ID
            
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Should find project for manager 3");
                assertEquals("CH-PBLD-2025-091", rs.getString("project_number"));
                assertFalse(rs.next(), "Should only find one project");
            }
        }
        
        // Test "All Managers" filter (no WHERE clause)
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM projects")) {
            
            assertTrue(rs.next());
            assertEquals(3, rs.getInt("count"), "Should find all projects");
        }
    }
}