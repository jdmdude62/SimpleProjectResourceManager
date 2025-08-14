package com.subliminalsearch.simpleprojectresourcemanager.regression;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import com.subliminalsearch.simpleprojectresourcemanager.model.Project;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

/**
 * Regression tests for date format bugs fixed in the application.
 * 
 * BUG-2341: Project disappears when PM changed due to date corruption
 * BUG-2339: Project visibility filter issues
 */
@DisplayName("Date Format Regression Tests - Prevents Date Corruption Issues")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DateFormatRegressionTest {
    
    private Connection connection;
    private AssignmentRepository assignmentRepository;
    private ProjectRepository projectRepository;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory database for testing
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
        // Create tables
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
                    notes TEXT,
                    FOREIGN KEY (project_id) REFERENCES projects(id)
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
        
        assignmentRepository = new AssignmentRepository();
        assignmentRepository.setConnection(connection);
        
        projectRepository = new ProjectRepository();
        projectRepository.setConnection(connection);
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
        Assignment assignment = new Assignment();
        assignment.setProjectId(1);
        assignment.setResourceId(2);
        assignment.setStartDate(LocalDate.of(2025, 8, 10));
        assignment.setEndDate(LocalDate.of(2025, 8, 20));
        assignment.setTravelOutDays(1);
        assignment.setTravelBackDays(1);
        
        // WHEN the assignment is saved
        assignmentRepository.save(assignment);
        
        // THEN the dates should be stored in correct format
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT start_date, end_date FROM assignments WHERE id = " + assignment.getId())) {
            
            assertThat(rs.next()).isTrue();
            String storedStartDate = rs.getString("start_date");
            String storedEndDate = rs.getString("end_date");
            
            // Verify correct format: "YYYY-MM-DD HH:mm:ss.SSS"
            assertThat(storedStartDate)
                .as("Start date should be in correct format, not timestamp")
                .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
                .contains("2025-08-10");
            
            assertThat(storedEndDate)
                .as("End date should be in correct format, not timestamp")
                .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
                .contains("2025-08-20");
            
            // Verify NOT stored as timestamp
            assertThat(storedStartDate)
                .as("Should not be stored as timestamp")
                .doesNotMatch("\\d{13}"); // Unix timestamp in milliseconds
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("BUG-2341: Should correctly retrieve assignments after date update")
    void shouldCorrectlyRetrieveAssignmentsAfterDateUpdate() throws Exception {
        // GIVEN a project and assignment
        Project project = new Project();
        project.setName("Test Project CH-PBLD-2025-091");
        project.setProjectNumber("CH-PBLD-2025-091");
        project.setStartDate(LocalDate.of(2025, 8, 1));
        project.setEndDate(LocalDate.of(2025, 8, 31));
        project.setProjectManagerId(1);
        projectRepository.save(project);
        
        Assignment assignment = new Assignment();
        assignment.setProjectId(project.getId());
        assignment.setResourceId(2);
        assignment.setStartDate(LocalDate.of(2025, 8, 10));
        assignment.setEndDate(LocalDate.of(2025, 8, 20));
        assignmentRepository.save(assignment);
        
        // WHEN the assignment dates are updated
        assignment.setEndDate(LocalDate.of(2025, 8, 25));
        assignmentRepository.update(assignment);
        
        // THEN the assignment should still be retrievable
        Assignment retrieved = assignmentRepository.findById(assignment.getId());
        assertThat(retrieved)
            .as("Assignment should be retrievable after date update")
            .isNotNull();
        
        assertThat(retrieved.getEndDate())
            .as("Updated end date should be correct")
            .isEqualTo(LocalDate.of(2025, 8, 25));
        
        // AND the project should still be visible in date range queries
        var projectsInRange = projectRepository.findByDateRange(
            LocalDate.of(2025, 8, 1),
            LocalDate.of(2025, 8, 31)
        );
        
        assertThat(projectsInRange)
            .as("Project should be visible in date range after assignment update")
            .anyMatch(p -> p.getId() == project.getId());
    }
    
    @Test
    @Order(3)
    @DisplayName("BUG-2341: Should handle project manager change without losing visibility")
    void shouldHandleProjectManagerChangeWithoutLosingVisibility() throws Exception {
        // GIVEN a project with assignments
        Project project = new Project();
        project.setName("Test Project");
        project.setProjectNumber("TEST-001");
        project.setStartDate(LocalDate.of(2025, 8, 1));
        project.setEndDate(LocalDate.of(2025, 8, 31));
        project.setProjectManagerId(1); // Original PM
        projectRepository.save(project);
        
        Assignment assignment = new Assignment();
        assignment.setProjectId(project.getId());
        assignment.setResourceId(2);
        assignment.setStartDate(LocalDate.of(2025, 8, 10));
        assignment.setEndDate(LocalDate.of(2025, 8, 20));
        assignmentRepository.save(assignment);
        
        // WHEN the project manager is changed
        project.setProjectManagerId(3); // Paula Poodle's ID
        projectRepository.update(project);
        
        // THEN the project should still be visible
        Project retrieved = projectRepository.findById(project.getId());
        assertThat(retrieved)
            .as("Project should be retrievable after PM change")
            .isNotNull();
        
        assertThat(retrieved.getProjectManagerId())
            .as("PM should be updated")
            .isEqualTo(3);
        
        // AND assignments should still be valid
        Assignment retrievedAssignment = assignmentRepository.findById(assignment.getId());
        assertThat(retrievedAssignment)
            .as("Assignment should still be valid after PM change")
            .isNotNull();
        
        assertThat(retrievedAssignment.getStartDate())
            .as("Assignment dates should not be corrupted")
            .isEqualTo(LocalDate.of(2025, 8, 10));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should prevent date format corruption during batch updates")
    void shouldPreventDateCorruptionDuringBatchUpdates() throws Exception {
        // GIVEN multiple assignments
        for (int i = 1; i <= 10; i++) {
            Assignment assignment = new Assignment();
            assignment.setProjectId(1);
            assignment.setResourceId(i);
            assignment.setStartDate(LocalDate.of(2025, 8, i));
            assignment.setEndDate(LocalDate.of(2025, 8, i + 10));
            assignmentRepository.save(assignment);
        }
        
        // WHEN updating all assignments
        var allAssignments = assignmentRepository.findByProjectId(1);
        for (Assignment assignment : allAssignments) {
            assignment.setEndDate(assignment.getEndDate().plusDays(5));
            assignmentRepository.update(assignment);
        }
        
        // THEN all dates should remain in correct format
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT start_date, end_date FROM assignments WHERE project_id = 1")) {
            
            while (rs.next()) {
                String startDate = rs.getString("start_date");
                String endDate = rs.getString("end_date");
                
                assertThat(startDate)
                    .as("All start dates should be in correct format")
                    .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
                
                assertThat(endDate)
                    .as("All end dates should be in correct format")
                    .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
            }
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("Should handle edge case dates correctly")
    void shouldHandleEdgeCaseDatesCorrectly() throws Exception {
        // Test various edge cases that might cause issues
        LocalDate[] testDates = {
            LocalDate.of(2025, 1, 1),   // New Year
            LocalDate.of(2025, 2, 28),  // End of February
            LocalDate.of(2025, 2, 29),  // Leap year date (2025 is not leap year, should handle gracefully)
            LocalDate.of(2025, 12, 31), // End of year
            LocalDate.of(2024, 2, 29),  // Actual leap year date
        };
        
        for (int i = 0; i < testDates.length - 1; i++) {
            try {
                Assignment assignment = new Assignment();
                assignment.setProjectId(1);
                assignment.setResourceId(i + 1);
                assignment.setStartDate(testDates[i]);
                assignment.setEndDate(testDates[i].plusDays(10));
                
                assignmentRepository.save(assignment);
                
                Assignment retrieved = assignmentRepository.findById(assignment.getId());
                assertThat(retrieved)
                    .as("Should handle edge case date: " + testDates[i])
                    .isNotNull();
                
                assertThat(retrieved.getStartDate())
                    .as("Edge case date should be preserved correctly")
                    .isEqualTo(testDates[i]);
                    
            } catch (Exception e) {
                // Handle invalid dates gracefully
                if (testDates[i].equals(LocalDate.of(2025, 2, 29))) {
                    // Expected - 2025 is not a leap year
                    continue;
                }
                throw e;
            }
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("BUG-2339: Project filter should work correctly with all managers")
    void shouldFilterProjectsCorrectlyByManager() throws Exception {
        // GIVEN projects with different managers
        Project project1 = createProject("PROJ-001", 1);
        Project project2 = createProject("PROJ-002", 2);
        Project project3 = createProject("PROJ-003", 3);
        Project project4 = createProject("PROJ-004", null); // Unassigned
        
        projectRepository.save(project1);
        projectRepository.save(project2);
        projectRepository.save(project3);
        projectRepository.save(project4);
        
        // WHEN filtering by specific manager
        var managerProjects = projectRepository.findByManagerId(3);
        
        // THEN should return only that manager's projects
        assertThat(managerProjects)
            .as("Should return only projects for manager 3")
            .hasSize(1)
            .allMatch(p -> p.getProjectManagerId() != null && p.getProjectManagerId() == 3);
        
        // WHEN filtering for all managers
        var allProjects = projectRepository.findAll();
        
        // THEN should return all projects
        assertThat(allProjects)
            .as("Should return all projects when 'All Managers' selected")
            .hasSize(4);
        
        // WHEN filtering for unassigned
        var unassignedProjects = projectRepository.findByManagerId(null);
        
        // THEN should return only unassigned projects
        assertThat(unassignedProjects)
            .as("Should return only unassigned projects")
            .hasSize(1)
            .allMatch(p -> p.getProjectManagerId() == null);
    }
    
    // Helper methods
    private Project createProject(String number, Integer managerId) {
        Project project = new Project();
        project.setName("Test " + number);
        project.setProjectNumber(number);
        project.setStartDate(LocalDate.of(2025, 8, 1));
        project.setEndDate(LocalDate.of(2025, 8, 31));
        project.setProjectManagerId(managerId);
        return project;
    }
}