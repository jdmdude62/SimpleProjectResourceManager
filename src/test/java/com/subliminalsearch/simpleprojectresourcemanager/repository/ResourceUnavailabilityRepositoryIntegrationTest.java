package com.subliminalsearch.simpleprojectresourcemanager.repository;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.util.TestDatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Resource Unavailability Repository Integration Tests")
class ResourceUnavailabilityRepositoryIntegrationTest {
    
    private static HikariDataSource dataSource;
    private ResourceUnavailabilityRepository repository;
    private ResourceRepository resourceRepository;
    private Resource testResource;
    
    @BeforeAll
    static void setUpDatabase() {
        dataSource = TestDatabaseConfig.createTestDataSource();
    }
    
    @AfterAll
    static void tearDownDatabase() {
        if (dataSource != null) {
            dataSource.close();
        }
        TestDatabaseConfig.cleanupTestDatabase();
    }
    
    @BeforeEach
    void setUp() throws Exception {
        // Create repositories
        repository = new ResourceUnavailabilityRepository(dataSource);
        resourceRepository = new ResourceRepository(dataSource);
        
        if (TestDatabaseConfig.isUsingProductionCopy()) {
            // Using production data - just use the first available resource
            List<Resource> existingResources = resourceRepository.findAll();
            if (!existingResources.isEmpty()) {
                testResource = existingResources.get(0);
                System.out.println("Using existing resource from production: " + testResource.getName());
            } else {
                // Fallback: create test resource if production DB has no resources
                testResource = new Resource("Test User", "test@example.com", new ResourceType("Technician", ResourceCategory.INTERNAL));
                testResource = resourceRepository.save(testResource);
            }
        } else {
            // Using test database - clear and create fresh data
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS resource_unavailability");
                stmt.execute("DROP TABLE IF EXISTS resources");
            }
            
            // Create test resource
            testResource = new Resource("John Doe", "john@example.com", new ResourceType("Technician", ResourceCategory.INTERNAL));
            testResource = resourceRepository.save(testResource);
        }
    }
    
    @Test
    @DisplayName("Should create and retrieve unavailability")
    void testCreateAndRetrieve() {
        // Given
        TechnicianUnavailability unavailability = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 7, 14)
        );
        unavailability.setReason("Summer vacation");
        
        // When
        TechnicianUnavailability saved = repository.save(unavailability);
        Optional<TechnicianUnavailability> retrieved = repository.findById(saved.getId());
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getResourceId()).isEqualTo(testResource.getId());
        assertThat(retrieved.get().getType()).isEqualTo(UnavailabilityType.VACATION);
        assertThat(retrieved.get().getReason()).isEqualTo("Summer vacation");
    }
    
    @Test
    @DisplayName("Should find unavailabilities by resource")
    void testFindByResourceId() {
        // Given
        TechnicianUnavailability vacation = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 7, 14)
        );
        
        TechnicianUnavailability training = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 5)
        );
        
        repository.save(vacation);
        repository.save(training);
        
        // When
        List<TechnicianUnavailability> unavailabilities = repository.findByResourceId(testResource.getId());
        
        // Then
        assertThat(unavailabilities).hasSize(2);
        assertThat(unavailabilities)
            .extracting(TechnicianUnavailability::getType)
            .containsExactlyInAnyOrder(UnavailabilityType.VACATION, UnavailabilityType.TRAINING);
    }
    
    @Test
    @DisplayName("Should find overlapping unavailabilities")
    void testFindOverlapping() {
        // Given
        TechnicianUnavailability existing = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 10),
            LocalDate.of(2024, 7, 20)
        );
        repository.save(existing);
        
        // When - check for overlapping period
        List<TechnicianUnavailability> overlapping = repository.findOverlapping(
            testResource.getId(),
            LocalDate.of(2024, 7, 15),
            LocalDate.of(2024, 7, 25)
        );
        
        // Then
        assertThat(overlapping).hasSize(1);
        assertThat(overlapping.get(0).getType()).isEqualTo(UnavailabilityType.VACATION);
    }
    
    @Test
    @DisplayName("Should find unavailabilities by date range")
    void testFindByDateRange() {
        // Given
        TechnicianUnavailability july = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 7, 14)
        );
        
        TechnicianUnavailability august = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 5)
        );
        
        TechnicianUnavailability september = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.SICK_LEAVE,
            LocalDate.of(2024, 9, 10),
            LocalDate.of(2024, 9, 12)
        );
        
        repository.save(july);
        repository.save(august);
        repository.save(september);
        
        // When - query for July and August
        List<TechnicianUnavailability> result = repository.findByDateRange(
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 8, 31)
        );
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(TechnicianUnavailability::getType)
            .containsExactlyInAnyOrder(UnavailabilityType.VACATION, UnavailabilityType.TRAINING);
    }
    
    @Test
    @DisplayName("Should approve unavailability")
    void testApproveUnavailability() {
        // Given
        TechnicianUnavailability pending = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 14)
        );
        pending.setApproved(false);
        TechnicianUnavailability saved = repository.save(pending);
        
        // When
        repository.approveUnavailability(saved.getId(), "Manager");
        Optional<TechnicianUnavailability> approved = repository.findById(saved.getId());
        
        // Then
        assertThat(approved).isPresent();
        assertThat(approved.get().isApproved()).isTrue();
        assertThat(approved.get().getApprovedBy()).isEqualTo("Manager");
    }
    
    @Test
    @DisplayName("Should find pending approvals")
    void testFindPendingApproval() {
        // Given
        TechnicianUnavailability approved = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 7, 14)
        );
        approved.setApproved(true);
        
        TechnicianUnavailability pending1 = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 5)
        );
        pending1.setApproved(false);
        
        TechnicianUnavailability pending2 = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.PERSONAL_TIME,
            LocalDate.of(2024, 9, 1),
            LocalDate.of(2024, 9, 2)
        );
        pending2.setApproved(false);
        
        repository.save(approved);
        repository.save(pending1);
        repository.save(pending2);
        
        // When
        List<TechnicianUnavailability> pending = repository.findPendingApproval();
        
        // Then
        assertThat(pending).hasSize(2);
        assertThat(pending)
            .extracting(TechnicianUnavailability::isApproved)
            .containsOnly(false);
    }
    
    @Test
    @DisplayName("Should delete unavailability")
    void testDelete() {
        // Given
        TechnicianUnavailability unavailability = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 7, 14)
        );
        TechnicianUnavailability saved = repository.save(unavailability);
        
        // When
        repository.delete(saved.getId());
        Optional<TechnicianUnavailability> deleted = repository.findById(saved.getId());
        
        // Then
        assertThat(deleted).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle recurring unavailability")
    void testRecurringUnavailability() {
        // Given
        TechnicianUnavailability recurring = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.RECURRING,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31)
        );
        recurring.setRecurring(true);
        recurring.setRecurrencePattern("WEEKLY:FRIDAY");
        recurring.setReason("Weekly day off");
        
        // When
        TechnicianUnavailability saved = repository.save(recurring);
        Optional<TechnicianUnavailability> retrieved = repository.findById(saved.getId());
        
        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().isRecurring()).isTrue();
        assertThat(retrieved.get().getRecurrencePattern()).isEqualTo("WEEKLY:FRIDAY");
    }
    
    @Test
    @DisplayName("Should not find non-overlapping unavailabilities")
    void testNoOverlap() {
        // Given
        TechnicianUnavailability existing = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 1),
            LocalDate.of(2024, 7, 14)
        );
        repository.save(existing);
        
        // When - check for non-overlapping period
        List<TechnicianUnavailability> overlapping = repository.findOverlapping(
            testResource.getId(),
            LocalDate.of(2024, 8, 1),
            LocalDate.of(2024, 8, 14)
        );
        
        // Then
        assertThat(overlapping).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle edge case date overlaps")
    void testEdgeCaseDateOverlaps() {
        // Given
        TechnicianUnavailability existing = new TechnicianUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.of(2024, 7, 10),
            LocalDate.of(2024, 7, 20)
        );
        repository.save(existing);
        
        // Test exact boundary overlap (end date = start date)
        List<TechnicianUnavailability> endBoundary = repository.findOverlapping(
            testResource.getId(),
            LocalDate.of(2024, 7, 20),
            LocalDate.of(2024, 7, 25)
        );
        
        // Test exact boundary overlap (start date = end date)  
        List<TechnicianUnavailability> startBoundary = repository.findOverlapping(
            testResource.getId(),
            LocalDate.of(2024, 7, 5),
            LocalDate.of(2024, 7, 10)
        );
        
        // Then
        assertThat(endBoundary).hasSize(1);
        assertThat(startBoundary).hasSize(1);
    }
}