package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.Assignment;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Assignment Repository Tests - Data Access Layer")
class AssignmentRepositoryTest {
    
    private Connection connection;
    private AssignmentRepository repository;
    
    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
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
        }
        
        repository = new AssignmentRepository();
        repository.setConnection(connection);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Test
    @DisplayName("Should save assignment with all fields")
    void shouldSaveAssignmentWithAllFields() {
        Assignment assignment = new Assignment();
        assignment.setProjectId(1);
        assignment.setResourceId(2);
        assignment.setStartDate(LocalDate.of(2025, 8, 10));
        assignment.setEndDate(LocalDate.of(2025, 8, 20));
        assignment.setTravelOutDays(2);
        assignment.setTravelBackDays(1);
        assignment.setNotes("Test assignment");
        
        Assignment saved = repository.save(assignment);
        
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getProjectId()).isEqualTo(1);
        assertThat(saved.getResourceId()).isEqualTo(2);
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 10));
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2025, 8, 20));
        assertThat(saved.getTravelOutDays()).isEqualTo(2);
        assertThat(saved.getTravelBackDays()).isEqualTo(1);
        assertThat(saved.getNotes()).isEqualTo("Test assignment");
    }
    
    @Test
    @DisplayName("Should find assignment by ID")
    void shouldFindAssignmentById() {
        Assignment assignment = createTestAssignment();
        Assignment saved = repository.save(assignment);
        
        Assignment found = repository.findById(saved.getId());
        
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getProjectId()).isEqualTo(saved.getProjectId());
    }
    
    @Test
    @DisplayName("Should find assignments by project ID")
    void shouldFindAssignmentsByProjectId() {
        int projectId = 1;
        
        for (int i = 0; i < 5; i++) {
            Assignment assignment = createTestAssignment();
            assignment.setProjectId(projectId);
            assignment.setResourceId(i + 1);
            repository.save(assignment);
        }
        
        List<Assignment> assignments = repository.findByProjectId(projectId);
        
        assertThat(assignments).hasSize(5);
        assertThat(assignments).allMatch(a -> a.getProjectId() == projectId);
    }
    
    @Test
    @DisplayName("Should find assignments by resource ID")
    void shouldFindAssignmentsByResourceId() {
        int resourceId = 2;
        
        for (int i = 0; i < 3; i++) {
            Assignment assignment = createTestAssignment();
            assignment.setProjectId(i + 1);
            assignment.setResourceId(resourceId);
            repository.save(assignment);
        }
        
        List<Assignment> assignments = repository.findByResourceId(resourceId);
        
        assertThat(assignments).hasSize(3);
        assertThat(assignments).allMatch(a -> a.getResourceId() == resourceId);
    }
    
    @Test
    @DisplayName("Should find assignments in date range")
    void shouldFindAssignmentsInDateRange() {
        Assignment assignment1 = createTestAssignment();
        assignment1.setStartDate(LocalDate.of(2025, 8, 1));
        assignment1.setEndDate(LocalDate.of(2025, 8, 10));
        repository.save(assignment1);
        
        Assignment assignment2 = createTestAssignment();
        assignment2.setStartDate(LocalDate.of(2025, 8, 15));
        assignment2.setEndDate(LocalDate.of(2025, 8, 25));
        repository.save(assignment2);
        
        Assignment assignment3 = createTestAssignment();
        assignment3.setStartDate(LocalDate.of(2025, 9, 1));
        assignment3.setEndDate(LocalDate.of(2025, 9, 10));
        repository.save(assignment3);
        
        List<Assignment> augustAssignments = repository.findByDateRange(
            LocalDate.of(2025, 8, 1),
            LocalDate.of(2025, 8, 31)
        );
        
        assertThat(augustAssignments).hasSize(2);
        assertThat(augustAssignments)
            .allMatch(a -> !a.getStartDate().isBefore(LocalDate.of(2025, 8, 1)) 
                       && !a.getEndDate().isAfter(LocalDate.of(2025, 8, 31)));
    }
    
    @Test
    @DisplayName("Should update assignment")
    void shouldUpdateAssignment() {
        Assignment assignment = createTestAssignment();
        Assignment saved = repository.save(assignment);
        
        saved.setEndDate(LocalDate.of(2025, 8, 25));
        saved.setNotes("Updated notes");
        saved.setTravelBackDays(3);
        
        boolean updated = repository.update(saved);
        
        assertThat(updated).isTrue();
        
        Assignment found = repository.findById(saved.getId());
        assertThat(found.getEndDate()).isEqualTo(LocalDate.of(2025, 8, 25));
        assertThat(found.getNotes()).isEqualTo("Updated notes");
        assertThat(found.getTravelBackDays()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should delete assignment")
    void shouldDeleteAssignment() {
        Assignment assignment = createTestAssignment();
        Assignment saved = repository.save(assignment);
        
        boolean deleted = repository.delete(saved.getId());
        
        assertThat(deleted).isTrue();
        
        Assignment found = repository.findById(saved.getId());
        assertThat(found).isNull();
    }
    
    @Test
    @DisplayName("Should detect conflicts for same resource")
    void shouldDetectConflictsForSameResource() {
        Assignment assignment1 = createTestAssignment();
        assignment1.setResourceId(1);
        assignment1.setStartDate(LocalDate.of(2025, 8, 10));
        assignment1.setEndDate(LocalDate.of(2025, 8, 20));
        repository.save(assignment1);
        
        Assignment assignment2 = createTestAssignment();
        assignment2.setResourceId(1);
        assignment2.setStartDate(LocalDate.of(2025, 8, 15));
        assignment2.setEndDate(LocalDate.of(2025, 8, 25));
        
        List<Assignment> conflicts = repository.findConflicts(assignment2);
        
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(assignment1.getId());
    }
    
    @Test
    @DisplayName("Should not detect conflicts for different resources")
    void shouldNotDetectConflictsForDifferentResources() {
        Assignment assignment1 = createTestAssignment();
        assignment1.setResourceId(1);
        assignment1.setStartDate(LocalDate.of(2025, 8, 10));
        assignment1.setEndDate(LocalDate.of(2025, 8, 20));
        repository.save(assignment1);
        
        Assignment assignment2 = createTestAssignment();
        assignment2.setResourceId(2); // Different resource
        assignment2.setStartDate(LocalDate.of(2025, 8, 15));
        assignment2.setEndDate(LocalDate.of(2025, 8, 25));
        
        List<Assignment> conflicts = repository.findConflicts(assignment2);
        
        assertThat(conflicts).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle null resource ID")
    void shouldHandleNullResourceId() {
        Assignment assignment = createTestAssignment();
        assignment.setResourceId(null);
        
        Assignment saved = repository.save(assignment);
        
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getResourceId()).isNull();
        
        Assignment found = repository.findById(saved.getId());
        assertThat(found).isNotNull();
        assertThat(found.getResourceId()).isNull();
    }
    
    private Assignment createTestAssignment() {
        Assignment assignment = new Assignment();
        assignment.setProjectId(1);
        assignment.setResourceId(2);
        assignment.setStartDate(LocalDate.of(2025, 8, 10));
        assignment.setEndDate(LocalDate.of(2025, 8, 20));
        assignment.setTravelOutDays(0);
        assignment.setTravelBackDays(0);
        return assignment;
    }
}