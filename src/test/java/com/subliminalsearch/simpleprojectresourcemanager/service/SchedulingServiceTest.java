package com.subliminalsearch.simpleprojectresourcemanager.service;

import com.subliminalsearch.simpleprojectresourcemanager.config.DatabaseConfig;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.AssignmentRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ResourceRepository;
import com.subliminalsearch.simpleprojectresourcemanager.repository.ProjectManagerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceTest {
    private DatabaseConfig databaseConfig;
    private SchedulingService schedulingService;
    private ProjectRepository projectRepository;
    private ResourceRepository resourceRepository;
    private AssignmentRepository assignmentRepository;
    private ProjectManagerRepository projectManagerRepository;
    private Path testDbPath;

    @BeforeEach
    void setUp() throws IOException {
        testDbPath = Files.createTempDirectory("test-scheduler-service-db");
        databaseConfig = new DatabaseConfig(testDbPath.toString() + "/");
        
        projectRepository = new ProjectRepository(databaseConfig.getDataSource());
        resourceRepository = new ResourceRepository(databaseConfig.getDataSource());
        assignmentRepository = new AssignmentRepository(databaseConfig.getDataSource());
        projectManagerRepository = new ProjectManagerRepository(databaseConfig.getDataSource());
        
        schedulingService = new SchedulingService(projectRepository, resourceRepository, assignmentRepository, 
                                                  projectManagerRepository, databaseConfig.getDataSource());
    }

    @AfterEach
    void tearDown() throws IOException {
        databaseConfig.shutdown();
        Files.deleteIfExists(Paths.get(testDbPath.toString(), "scheduler.db"));
        Files.deleteIfExists(testDbPath);
    }

    @Test
    void shouldCreateProjectSuccessfully() {
        Project project = schedulingService.createProject("PROJ-001", "Test Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        assertNotNull(project.getId());
        assertEquals("PROJ-001", project.getProjectId());
        assertEquals("Test Project", project.getDescription());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void shouldPreventDuplicateProjectIds() {
        schedulingService.createProject("PROJ-001", "First Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.createProject("PROJ-001", "Second Project",
                    LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 20));
        });
    }

    @Test
    void shouldCreateResourceSuccessfully() {
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john.doe@company.com", resourceType);
        
        assertNotNull(resource.getId());
        assertEquals("John Doe", resource.getName());
        assertEquals("john.doe@company.com", resource.getEmail());
        assertTrue(resource.isActive());
    }

    @Test
    void shouldCreateAssignmentSuccessfully() {
        // Setup
        Project project = schedulingService.createProject("PROJ-001", "Test Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john.doe@company.com", resourceType);
        
        // Test
        Assignment assignment = schedulingService.createAssignment(
                project.getId(), resource.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10),
                1, 1);
        
        assertNotNull(assignment.getId());
        assertEquals(project.getId(), assignment.getProjectId());
        assertEquals(resource.getId(), assignment.getResourceId());
        assertEquals(LocalDate.of(2025, 8, 5), assignment.getStartDate());
        assertEquals(LocalDate.of(2025, 8, 10), assignment.getEndDate());
        assertEquals(1, assignment.getTravelOutDays());
        assertEquals(1, assignment.getTravelBackDays());
        assertFalse(assignment.isOverride());
    }

    @Test
    void shouldDetectResourceConflicts() {
        // Setup
        Project project1 = schedulingService.createProject("PROJ-001", "First Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        Project project2 = schedulingService.createProject("PROJ-002", "Second Project",
                LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 25));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john.doe@company.com", resourceType);
        
        // Create first assignment
        schedulingService.createAssignment(project1.getId(), resource.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10));
        
        // Test: Attempt to create overlapping assignment should fail
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.createAssignment(project2.getId(), resource.getId(),
                    LocalDate.of(2025, 8, 8), LocalDate.of(2025, 8, 12));
        });
    }

    @Test
    void shouldAllowAssignmentWithOverride() {
        // Setup
        Project project1 = schedulingService.createProject("PROJ-001", "First Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        Project project2 = schedulingService.createProject("PROJ-002", "Second Project",
                LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 25));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john.doe@company.com", resourceType);
        
        // Create first assignment  
        schedulingService.createAssignment(project1.getId(), resource.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 12));
        
        // Test: Create overlapping assignment with override should succeed
        // This assignment overlaps with the first one but is within project2's date range
        Assignment overrideAssignment = schedulingService.createAssignmentWithOverride(
                project2.getId(), resource.getId(),
                LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 15),
                1, 1, "Emergency project approved by manager");
        
        assertNotNull(overrideAssignment.getId());
        assertTrue(overrideAssignment.isOverride());
        assertEquals("Emergency project approved by manager", overrideAssignment.getOverrideReason());
    }

    @Test
    void shouldValidateProjectDateBoundaries() {
        // Setup
        Project project = schedulingService.createProject("PROJ-001", "Test Project",
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 15));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john.doe@company.com", resourceType);
        
        // Test: Assignment outside project dates should fail
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.createAssignment(project.getId(), resource.getId(),
                    LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 10)); // Starts before project
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.createAssignment(project.getId(), resource.getId(),
                    LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 20)); // Ends after project
        });
    }

    @Test
    void shouldCheckResourceAvailability() {
        // Setup
        Project project = schedulingService.createProject("PROJ-001", "Test Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john.doe@company.com", resourceType);
        
        // Resource should be available initially
        assertTrue(schedulingService.isResourceAvailable(resource.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10)));
        
        // Create assignment
        schedulingService.createAssignment(project.getId(), resource.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10));
        
        // Resource should no longer be available during that period
        assertFalse(schedulingService.isResourceAvailable(resource.getId(),
                LocalDate.of(2025, 8, 7), LocalDate.of(2025, 8, 12)));
        
        // But should be available outside that period
        assertTrue(schedulingService.isResourceAvailable(resource.getId(),
                LocalDate.of(2025, 8, 12), LocalDate.of(2025, 8, 15)));
    }

    @Test
    void shouldRetrieveProjectsByDateRange() {
        // Setup
        schedulingService.createProject("PROJ-001", "August Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        schedulingService.createProject("PROJ-002", "September Project",
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 15));
        schedulingService.createProject("PROJ-003", "Overlapping Project",
                LocalDate.of(2025, 8, 15), LocalDate.of(2025, 9, 5));
        
        // Test: Get projects in August
        List<Project> augustProjects = schedulingService.getProjectsByDateRange(
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31));
        
        assertEquals(2, augustProjects.size());
        assertTrue(augustProjects.stream().anyMatch(p -> p.getProjectId().equals("PROJ-001")));
        assertTrue(augustProjects.stream().anyMatch(p -> p.getProjectId().equals("PROJ-003")));
    }

    @Test
    void shouldCalculateStatistics() {
        // Setup
        Project project = schedulingService.createProject("PROJ-001", "Test Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource1 = schedulingService.createResource("John Doe", "john@company.com", resourceType);
        Resource resource2 = schedulingService.createResource("Jane Smith", "jane@company.com", resourceType);
        
        schedulingService.createAssignment(project.getId(), resource1.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10));
        schedulingService.createAssignment(project.getId(), resource2.getId(),
                LocalDate.of(2025, 8, 7), LocalDate.of(2025, 8, 12));
        
        // Test
        assertEquals(1, schedulingService.getProjectCount());
        assertEquals(2, schedulingService.getResourceCount());
        assertEquals(2, schedulingService.getAssignmentCount());
    }

    @Test
    void shouldPreventProjectDeletionWithExistingAssignments() {
        // Setup
        Project project = schedulingService.createProject("PROJ-001", "Test Project",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 15));
        
        ResourceType resourceType = new ResourceType("Full-Time Employee", ResourceCategory.INTERNAL);
        Resource resource = schedulingService.createResource("John Doe", "john@company.com", resourceType);
        
        schedulingService.createAssignment(project.getId(), resource.getId(),
                LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 10));
        
        // Test: Should not allow deletion with existing assignments
        assertThrows(IllegalArgumentException.class, () -> {
            schedulingService.deleteProject(project.getId());
        });
        
        // Clean up assignments and then deletion should work
        List<Assignment> assignments = schedulingService.getAssignmentsByProject(project.getId());
        for (Assignment assignment : assignments) {
            schedulingService.deleteAssignment(assignment.getId());
        }
        
        // Now deletion should succeed
        assertDoesNotThrow(() -> {
            schedulingService.deleteProject(project.getId());
        });
    }
}