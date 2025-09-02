package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Simple unit tests for SchedulingService using mocks
 */
@DisplayName("Scheduling Service Simple Tests")
public class SchedulingServiceSimpleTest {
    
    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private ResourceRepository resourceRepository;
    
    @Mock
    private AssignmentRepository assignmentRepository;
    
    @Mock
    private ProjectManagerRepository projectManagerRepository;
    
    @Mock
    private HikariDataSource dataSource;
    
    private SchedulingService schedulingService;
    private AutoCloseable mocks;
    
    @BeforeEach
    void setUp() throws SQLException {
        mocks = MockitoAnnotations.openMocks(this);
        
        // Pass null dataSource to prevent ResourceUnavailabilityRepository creation in tests
        // The service checks for null dataSource before creating the unavailability repository
        schedulingService = new SchedulingService(
            projectRepository,
            resourceRepository,
            assignmentRepository,
            projectManagerRepository,
            null  // Pass null to prevent unavailability repository issues
        );
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }
    
    @Test
    @DisplayName("Should create project successfully")
    void testCreateProject() {
        // Given
        String projectId = "PROJ-001";
        String description = "Test Project";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        
        Project expectedProject = new Project(projectId, description, startDate, endDate);
        expectedProject.setId(1L);
        
        when(projectRepository.save(any(Project.class))).thenReturn(expectedProject);
        
        // When
        Project result = schedulingService.createProject(projectId, description, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(description, result.getDescription());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        
        // Note: No longer checking for duplicate project IDs as per new implementation
        verify(projectRepository).save(any(Project.class));
    }
    
    @Test
    @DisplayName("Should allow duplicate project IDs")
    void testAllowDuplicateProjectIds() {
        // Given - duplicate project IDs are now allowed per new implementation
        String projectId = "PROJ-001";
        String description = "New Project";
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(10);
        
        Project expectedProject = new Project(projectId, description, startDate, endDate);
        expectedProject.setId(2L); // Different DB ID
        
        when(projectRepository.save(any(Project.class))).thenReturn(expectedProject);
        
        // When - Creating project with duplicate project ID should succeed
        Project result = schedulingService.createProject(projectId, description, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals(2L, result.getId());
        
        verify(projectRepository).save(any(Project.class));
    }
    
    @Test
    @DisplayName("Should check resource availability")
    void testCheckResourceAvailability() {
        // Given
        Long resourceId = 1L;
        LocalDate startDate = LocalDate.of(2025, 1, 10);
        LocalDate endDate = LocalDate.of(2025, 1, 20);
        
        // No overlapping assignments
        when(assignmentRepository.findOverlappingAssignments(resourceId, startDate, endDate))
            .thenReturn(Collections.emptyList());
        
        // When
        boolean available = schedulingService.isResourceAvailable(resourceId, startDate, endDate);
        
        // Then
        assertTrue(available);
        verify(assignmentRepository).findOverlappingAssignments(resourceId, startDate, endDate);
    }
    
    @Test
    @DisplayName("Should detect resource conflicts")
    void testDetectResourceConflicts() {
        // Given
        Long resourceId = 1L;
        LocalDate startDate = LocalDate.of(2025, 1, 15);
        LocalDate endDate = LocalDate.of(2025, 1, 25);
        
        // Existing assignment that overlaps
        Assignment existingAssignment = new Assignment();
        existingAssignment.setResourceId(resourceId);
        existingAssignment.setStartDate(LocalDate.of(2025, 1, 10));
        existingAssignment.setEndDate(LocalDate.of(2025, 1, 20));
        existingAssignment.setOverride(false); // Normal assignment, not an override
        
        when(assignmentRepository.findOverlappingAssignments(resourceId, startDate, endDate))
            .thenReturn(Arrays.asList(existingAssignment));
        
        // When
        boolean available = schedulingService.isResourceAvailable(resourceId, startDate, endDate);
        
        // Then
        assertFalse(available);
        verify(assignmentRepository).findOverlappingAssignments(resourceId, startDate, endDate);
    }
    
    @Test
    @DisplayName("Should get projects by date range")
    void testGetProjectsByDateRange() {
        // Given
        LocalDate rangeStart = LocalDate.of(2025, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2025, 3, 31);
        
        Project project1 = new Project("Q1-001", "Q1 Project", 
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31));
        Project project2 = new Project("Q1-002", "Partial Q1", 
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 4, 30));
        
        when(projectRepository.findByDateRange(rangeStart, rangeEnd))
            .thenReturn(Arrays.asList(project1, project2));
        
        // When
        List<Project> result = schedulingService.getProjectsByDateRange(rangeStart, rangeEnd);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(project1));
        assertTrue(result.contains(project2));
        
        verify(projectRepository).findByDateRange(rangeStart, rangeEnd);
    }
    
    @Test
    @DisplayName("Should get assignments by project")
    void testGetAssignmentsByProject() {
        // Given
        Long projectId = 1L;
        
        Assignment assignment1 = new Assignment();
        assignment1.setProjectId(projectId);
        assignment1.setResourceId(1L);
        
        Assignment assignment2 = new Assignment();
        assignment2.setProjectId(projectId);
        assignment2.setResourceId(2L);
        
        when(assignmentRepository.findByProjectId(projectId))
            .thenReturn(Arrays.asList(assignment1, assignment2));
        
        // When
        List<Assignment> result = schedulingService.getAssignmentsByProject(projectId);
        
        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(a -> a.getProjectId().equals(projectId)));
        
        verify(assignmentRepository).findByProjectId(projectId);
    }
    
    @Test
    @DisplayName("Should calculate project statistics")
    void testCalculateStatistics() {
        // Given
        when(projectRepository.count()).thenReturn(5L);
        when(resourceRepository.count()).thenReturn(10L);
        when(assignmentRepository.count()).thenReturn(15L);
        
        // When
        long projectCount = schedulingService.getProjectCount();
        long resourceCount = schedulingService.getResourceCount();
        long assignmentCount = schedulingService.getAssignmentCount();
        
        // Then
        assertEquals(5L, projectCount);
        assertEquals(10L, resourceCount);
        assertEquals(15L, assignmentCount);
        
        verify(projectRepository).count();
        verify(resourceRepository).count();
        verify(assignmentRepository).count();
    }
    
    @Test
    @DisplayName("Should validate assignment dates within project bounds")
    void testValidateAssignmentDates() {
        // Given
        Long projectId = 1L;
        Long resourceId = 1L;
        
        Project project = new Project("PROJ-001", "Test", 
            LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20));
        project.setId(projectId);
        
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(resourceRepository.existsById(resourceId)).thenReturn(true);
        when(assignmentRepository.findByResourceId(resourceId)).thenReturn(Collections.emptyList());
        
        // When & Then - Assignment outside project dates should fail
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.createAssignment(projectId, resourceId,
                LocalDate.of(2025, 1, 5), LocalDate.of(2025, 1, 15)); // Starts before project
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.createAssignment(projectId, resourceId,
                LocalDate.of(2025, 1, 15), LocalDate.of(2025, 1, 25)); // Ends after project
        });
    }
    
    @Test
    @DisplayName("Should delete assignment successfully")
    void testDeleteAssignment() {
        // Given
        Long assignmentId = 1L;
        
        when(assignmentRepository.existsById(assignmentId)).thenReturn(true);
        doNothing().when(assignmentRepository).delete(assignmentId);
        
        // When
        schedulingService.deleteAssignment(assignmentId);
        
        // Then
        verify(assignmentRepository).existsById(assignmentId);
        verify(assignmentRepository).delete(assignmentId);
    }
    
    @Test
    @DisplayName("Should prevent project deletion with assignments")
    void testPreventProjectDeletionWithAssignments() {
        // Given
        Long projectId = 1L;
        Project project = new Project("PROJ-001", "Test Project", LocalDate.now(), LocalDate.now());
        project.setId(projectId);
        
        Assignment assignment = new Assignment();
        assignment.setProjectId(projectId);
        
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(assignmentRepository.findByProjectId(projectId))
            .thenReturn(Arrays.asList(assignment));
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.deleteProject(projectId);
        });
        
        verify(projectRepository).findById(projectId);
        verify(assignmentRepository).findByProjectId(projectId);
        verify(projectRepository, never()).delete(projectId);
    }
}