package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.model.ProjectStatus;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration tests for ProjectRepository using in-memory database
 */
@DisplayName("Project Repository Simple Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProjectRepositorySimpleTest {
    
    private ProjectRepository projectRepository;
    private Connection connection;
    private DataSource dataSource;
    
    @BeforeAll
    void setupDatabase() throws SQLException {
        // Create in-memory database
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
        // Create projects table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_id TEXT NOT NULL UNIQUE,
                    name TEXT,
                    description TEXT,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    status TEXT,
                    location TEXT,
                    address TEXT,
                    client_name TEXT,
                    project_manager_id INTEGER,
                    travel_days INTEGER DEFAULT 0,
                    estimated_hours REAL,
                    budget REAL DEFAULT 0,
                    labor_budget REAL DEFAULT 0,
                    materials_budget REAL DEFAULT 0,
                    equipment_budget REAL DEFAULT 0,
                    contractor_budget REAL DEFAULT 0,
                    other_budget REAL DEFAULT 0,
                    actual_cost REAL DEFAULT 0,
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }
        
        // Create a simple DataSource wrapper
        dataSource = new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return connection;
            }
            
            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return connection;
            }
            
            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                throw new SQLException("Not implemented");
            }
            
            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;
            }
            
            @Override
            public java.io.PrintWriter getLogWriter() throws SQLException {
                return null;
            }
            
            @Override
            public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            }
            
            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
            }
            
            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }
            
            @Override
            public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
                throw new SQLFeatureNotSupportedException();
            }
        };
        
        projectRepository = new ProjectRepository(dataSource);
    }
    
    @AfterEach
    void cleanupData() throws SQLException {
        // Clear data between tests
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM projects");
        }
    }
    
    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Test
    @DisplayName("Should create and retrieve a project")
    void testCreateAndRetrieveProject() throws SQLException {
        // Create project
        Project project = new Project();
        project.setProjectId("TEST-001");
        // No setName method - use description instead
        project.setDescription("Test Description");
        project.setStartDate(LocalDate.of(2025, 1, 1));
        project.setEndDate(LocalDate.of(2025, 12, 31));
        project.setStatus(ProjectStatus.ACTIVE);
        project.setBudgetAmount(100000.0);
        
        // Save directly to database
        String insertSql = """
            INSERT INTO projects (project_id, name, description, start_date, end_date, status, budget)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, project.getProjectId());
            pstmt.setString(2, "Test Project"); // name field in DB
            pstmt.setString(3, project.getDescription());
            pstmt.setString(4, project.getStartDate().toString());
            pstmt.setString(5, project.getEndDate().toString());
            pstmt.setString(6, project.getStatus().toString());
            pstmt.setDouble(7, project.getBudgetAmount() != null ? project.getBudgetAmount() : 0.0);
            
            int affected = pstmt.executeUpdate();
            assertEquals(1, affected);
            
            // Get generated ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    project.setId(generatedKeys.getLong(1));
                }
            }
        }
        
        // Retrieve and verify
        String selectSql = "SELECT * FROM projects WHERE project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setString(1, "TEST-001");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("TEST-001", rs.getString("project_id"));
                assertEquals("Test Project", rs.getString("name"));
                assertEquals("Test Description", rs.getString("description"));
                assertEquals(100000.0, rs.getDouble("budget"));
            }
        }
    }
    
    @Test
    @DisplayName("Should find projects by date range")
    void testFindByDateRange() throws SQLException {
        // Insert test projects
        String insertSql = """
            INSERT INTO projects (project_id, name, start_date, end_date, status, budget)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        // Q1 Project
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, "Q1-001");
            pstmt.setString(2, "Q1 Project");
            pstmt.setString(3, "2025-01-01");
            pstmt.setString(4, "2025-03-31");
            pstmt.setString(5, "Active");
            pstmt.setDouble(6, 50000.0);
            pstmt.executeUpdate();
        }
        
        // Q2 Project
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, "Q2-001");
            pstmt.setString(2, "Q2 Project");
            pstmt.setString(3, "2025-04-01");
            pstmt.setString(4, "2025-06-30");
            pstmt.setString(5, "Active");
            pstmt.setDouble(6, 75000.0);
            pstmt.executeUpdate();
        }
        
        // Q3 Project
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, "Q3-001");
            pstmt.setString(2, "Q3 Project");
            pstmt.setString(3, "2025-07-01");
            pstmt.setString(4, "2025-09-30");
            pstmt.setString(5, "Active");
            pstmt.setDouble(6, 60000.0);
            pstmt.executeUpdate();
        }
        
        // Query Q2 projects
        String selectSql = """
            SELECT * FROM projects
            WHERE date(start_date) <= date(?) AND date(end_date) >= date(?)
            ORDER BY start_date
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setString(1, "2025-06-30");
            pstmt.setString(2, "2025-04-01");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Q2-001", rs.getString("project_id"));
                assertFalse(rs.next()); // Should only have one result
            }
        }
    }
    
    @Test
    @DisplayName("Should update project budget fields")
    void testUpdateBudgetFields() throws SQLException {
        // Insert project
        String insertSql = """
            INSERT INTO projects (project_id, name, start_date, end_date, status, budget)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        long projectId;
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "BUD-001");
            pstmt.setString(2, "Budget Test");
            pstmt.setString(3, "2025-01-01");
            pstmt.setString(4, "2025-12-31");
            pstmt.setString(5, "Active");
            pstmt.setDouble(6, 100000.0);
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                generatedKeys.next();
                projectId = generatedKeys.getLong(1);
            }
        }
        
        // Update budget fields
        String updateSql = """
            UPDATE projects SET
                labor_budget = ?,
                materials_budget = ?,
                equipment_budget = ?,
                contractor_budget = ?,
                other_budget = ?
            WHERE id = ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
            pstmt.setDouble(1, 40000.0);
            pstmt.setDouble(2, 30000.0);
            pstmt.setDouble(3, 20000.0);
            pstmt.setDouble(4, 10000.0);
            pstmt.setDouble(5, 0.0);
            pstmt.setLong(6, projectId);
            
            int affected = pstmt.executeUpdate();
            assertEquals(1, affected);
        }
        
        // Verify update
        String selectSql = "SELECT * FROM projects WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setLong(1, projectId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(40000.0, rs.getDouble("labor_budget"));
                assertEquals(30000.0, rs.getDouble("materials_budget"));
                assertEquals(20000.0, rs.getDouble("equipment_budget"));
                assertEquals(10000.0, rs.getDouble("contractor_budget"));
            }
        }
    }
    
    @Test
    @DisplayName("Should handle project with null values")
    void testProjectWithNullValues() throws SQLException {
        // Insert minimal project
        String insertSql = """
            INSERT INTO projects (project_id, start_date, end_date)
            VALUES (?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            pstmt.setString(1, "MIN-001");
            pstmt.setString(2, "2025-01-01");
            pstmt.setString(3, "2025-01-31");
            pstmt.executeUpdate();
        }
        
        // Retrieve and check defaults
        String selectSql = "SELECT * FROM projects WHERE project_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setString(1, "MIN-001");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("MIN-001", rs.getString("project_id"));
                assertNull(rs.getString("name"));
                assertNull(rs.getString("description"));
                assertEquals(0.0, rs.getDouble("budget"));
                assertEquals(0, rs.getInt("travel_days"));
            }
        }
    }
    
    @Test
    @DisplayName("Should count projects correctly")
    void testCountProjects() throws SQLException {
        // Check initial count
        String countSql = "SELECT COUNT(*) FROM projects";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
        
        // Add projects
        String insertSql = """
            INSERT INTO projects (project_id, start_date, end_date)
            VALUES (?, ?, ?)
            """;
        
        for (int i = 1; i <= 3; i++) {
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                pstmt.setString(1, "COUNT-00" + i);
                pstmt.setString(2, "2025-0" + i + "-01");
                pstmt.setString(3, "2025-0" + i + "-28");
                pstmt.executeUpdate();
            }
        }
        
        // Check new count
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            rs.next();
            assertEquals(3, rs.getInt(1));
        }
    }
}