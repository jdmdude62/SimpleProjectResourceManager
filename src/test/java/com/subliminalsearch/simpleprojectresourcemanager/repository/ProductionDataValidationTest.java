package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.util.TestDatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Production Data Validation Tests")
class ProductionDataValidationTest {
    
    private static HikariDataSource dataSource;
    private ResourceUnavailabilityRepository unavailabilityRepository;
    private ResourceRepository resourceRepository;
    private ProjectRepository projectRepository;
    private AssignmentRepository assignmentRepository;
    
    @BeforeAll
    static void setUpDatabase() {
        dataSource = TestDatabaseConfig.createTestDataSource();
        // Only run these tests if using production data copy
        Assumptions.assumeTrue(TestDatabaseConfig.isUsingProductionCopy(), 
            "Skipping production validation tests - not using production database copy");
    }
    
    @AfterAll
    static void tearDownDatabase() {
        if (dataSource != null) {
            dataSource.close();
        }
        TestDatabaseConfig.cleanupTestDatabase();
    }
    
    @BeforeEach
    void setUp() {
        unavailabilityRepository = new ResourceUnavailabilityRepository(dataSource);
        resourceRepository = new ResourceRepository(dataSource);
        projectRepository = new ProjectRepository(dataSource);
        assignmentRepository = new AssignmentRepository(dataSource);
    }
    
    @Test
    @DisplayName("Should validate production database schema")
    void testDatabaseSchema() {
        // Test that all required tables exist and can be queried
        assertThatCode(() -> {
            resourceRepository.findAll();
            projectRepository.findAll();
            assignmentRepository.findAll();
            unavailabilityRepository.findAll();
        }).doesNotThrowAnyException();
        
        System.out.println("✓ All required tables are accessible");
    }
    
    @Test
    @DisplayName("Should validate data integrity")
    void testDataIntegrity() {
        List<Resource> resources = resourceRepository.findAll();
        List<Project> projects = projectRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findAll();
        List<TechnicianUnavailability> unavailabilities = unavailabilityRepository.findAll();
        
        System.out.println("Production data summary:");
        System.out.println("- Resources: " + resources.size());
        System.out.println("- Projects: " + projects.size());
        System.out.println("- Assignments: " + assignments.size());
        System.out.println("- Unavailabilities: " + unavailabilities.size());
        
        // Basic data validation
        assertThat(resources).isNotEmpty();
        
        // Validate that all assignments reference existing resources and projects
        for (Assignment assignment : assignments) {
            boolean resourceExists = resources.stream()
                .anyMatch(r -> r.getId().equals(assignment.getResourceId()));
            boolean projectExists = projects.stream()
                .anyMatch(p -> p.getId().equals(assignment.getProjectId()));
            
            assertThat(resourceExists)
                .withFailMessage("Assignment %d references non-existent resource %d", 
                    assignment.getId(), assignment.getResourceId())
                .isTrue();
            
            assertThat(projectExists)
                .withFailMessage("Assignment %d references non-existent project %d", 
                    assignment.getId(), assignment.getProjectId())
                .isTrue();
        }
        
        // Validate that all unavailabilities reference existing resources
        for (TechnicianUnavailability unavailability : unavailabilities) {
            boolean resourceExists = resources.stream()
                .anyMatch(r -> r.getId().equals(unavailability.getResourceId()));
            
            assertThat(resourceExists)
                .withFailMessage("Unavailability %d references non-existent resource %d", 
                    unavailability.getId(), unavailability.getResourceId())
                .isTrue();
        }
        
        System.out.println("✓ Data integrity validation passed");
    }
    
    @Test
    @DisplayName("Should test unavailability repository operations on production data")
    void testUnavailabilityOperations() {
        List<Resource> resources = resourceRepository.findAll();
        assertThat(resources).isNotEmpty();
        
        Resource testResource = resources.get(0);
        System.out.println("Testing unavailability operations with resource: " + testResource.getName());
        
        // Test basic queries without modifying data
        List<TechnicianUnavailability> resourceUnavailabilities = 
            unavailabilityRepository.findByResourceId(testResource.getId());
        
        List<TechnicianUnavailability> dateRangeUnavailabilities = 
            unavailabilityRepository.findByDateRange(LocalDate.now().minusDays(30), LocalDate.now().plusDays(30));
        
        List<TechnicianUnavailability> pendingApprovals = 
            unavailabilityRepository.findPendingApproval();
        
        System.out.println("- Resource unavailabilities: " + resourceUnavailabilities.size());
        System.out.println("- Date range unavailabilities: " + dateRangeUnavailabilities.size());
        System.out.println("- Pending approvals: " + pendingApprovals.size());
        
        // Validate data structure
        for (TechnicianUnavailability unavailability : resourceUnavailabilities) {
            assertThat(unavailability.getResourceId()).isEqualTo(testResource.getId());
            assertThat(unavailability.getStartDate()).isNotNull();
            assertThat(unavailability.getEndDate()).isNotNull();
            assertThat(unavailability.getType()).isNotNull();
        }
        
        System.out.println("✓ Unavailability repository operations work correctly");
    }
    
    @Test
    @DisplayName("Should validate business rules with production data")
    void testBusinessRulesValidation() {
        List<Assignment> assignments = assignmentRepository.findAll();
        List<TechnicianUnavailability> unavailabilities = unavailabilityRepository.findAll();
        
        int conflictCount = 0;
        
        // Check for conflicts between assignments and unavailabilities
        for (Assignment assignment : assignments) {
            for (TechnicianUnavailability unavailability : unavailabilities) {
                if (assignment.getResourceId().equals(unavailability.getResourceId())) {
                    // Check for date overlap
                    boolean overlaps = !assignment.getEndDate().isBefore(unavailability.getStartDate()) &&
                                     !unavailability.getEndDate().isBefore(assignment.getStartDate());
                    
                    if (overlaps) {
                        conflictCount++;
                        System.out.println("Conflict detected: Assignment " + assignment.getId() + 
                                         " overlaps with unavailability " + unavailability.getId() + 
                                         " for resource " + assignment.getResourceId());
                    }
                }
            }
        }
        
        System.out.println("Total conflicts found: " + conflictCount);
        System.out.println("✓ Business rules validation completed");
        
        // This is informational - conflicts might exist in production data
        // We don't assert no conflicts, just report them
    }
    
    @Test
    @DisplayName("Should test query performance with production data volume")
    void testQueryPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Test common query patterns
        unavailabilityRepository.findAll();
        unavailabilityRepository.findByDateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        unavailabilityRepository.findPendingApproval();
        
        List<Resource> resources = resourceRepository.findAll();
        if (!resources.isEmpty()) {
            unavailabilityRepository.findByResourceId(resources.get(0).getId());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Query performance test completed in " + duration + "ms");
        
        // Performance should be reasonable even with production data
        assertThat(duration).isLessThan(5000); // Should complete within 5 seconds
        
        System.out.println("✓ Query performance is acceptable");
    }
}