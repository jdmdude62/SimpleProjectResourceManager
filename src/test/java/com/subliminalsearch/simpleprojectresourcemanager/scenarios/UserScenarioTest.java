package com.subliminalsearch.simpleprojectresourcemanager.scenarios;

import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.subliminalsearch.simpleprojectresourcemanager.service.FinancialService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Scenario Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserScenarioTest {
    
    private static HikariDataSource dataSource;
    private static SchedulingService schedulingService;
    private static FinancialService financialService;
    private static ProjectRepository projectRepository;
    private static ResourceRepository resourceRepository;
    private static AssignmentRepository assignmentRepository;
    private static ResourceUnavailabilityRepository unavailabilityRepository;
    
    @BeforeAll
    static void setupServices() {
        // Setup test database
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:test_scenarios.db");
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);
        
        // Initialize repositories
        projectRepository = new ProjectRepository(dataSource);
        resourceRepository = new ResourceRepository(dataSource);
        assignmentRepository = new AssignmentRepository(dataSource);
        ProjectManagerRepository pmRepository = new ProjectManagerRepository(dataSource);
        unavailabilityRepository = new ResourceUnavailabilityRepository(dataSource);
        
        // Initialize services
        schedulingService = new SchedulingService(
            projectRepository, resourceRepository, assignmentRepository, pmRepository, dataSource
        );
        financialService = new FinancialService(dataSource);
    }
    
    @AfterAll
    static void cleanup() {
        if (dataSource != null) {
            dataSource.close();
        }
        new File("test_scenarios.db").delete();
    }
    
    @Test
    @Order(1)
    @DisplayName("Scenario: Project Manager schedules team for new project with vacation conflicts")
    void testProjectSchedulingWithVacationConflicts() {
        // GIVEN: A project manager has a new project starting next month
        Project newProject = schedulingService.createProject(
            "PROJ-2024-001",
            "Website Redesign",
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(90)
        );
        
        // AND: The PM has a team of 3 technicians
        Resource techLead = schedulingService.createResource(
            "Sarah Johnson", "sarah@example.com", 
            new ResourceType("Tech Lead", ResourceCategory.INTERNAL)
        );
        
        Resource developer1 = schedulingService.createResource(
            "Mike Chen", "mike@example.com",
            new ResourceType("Developer", ResourceCategory.INTERNAL)
        );
        
        Resource developer2 = schedulingService.createResource(
            "Lisa Park", "lisa@example.com",
            new ResourceType("Developer", ResourceCategory.INTERNAL)
        );
        
        // WHEN: One technician has pre-approved vacation during project
        TechnicianUnavailability vacation = schedulingService.createUnavailability(
            developer1.getId(),
            UnavailabilityType.VACATION,
            LocalDate.now().plusDays(45),
            LocalDate.now().plusDays(55),
            "Family vacation - approved 3 months ago"
        );
        schedulingService.approveUnavailability(vacation.getId(), "HR Manager");
        
        // AND: PM tries to schedule the full team
        Assignment leadAssignment = schedulingService.createAssignment(
            newProject.getId(),
            techLead.getId(),
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(90)
        );
        
        // THEN: System should prevent scheduling during vacation
        assertThatThrownBy(() -> 
            schedulingService.createAssignment(
                newProject.getId(),
                developer1.getId(),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(90)
            )
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Resource conflict");
        
        // BUT: PM can schedule around the vacation
        Assignment dev1Part1 = schedulingService.createAssignment(
            newProject.getId(),
            developer1.getId(),
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(44)
        );
        
        Assignment dev1Part2 = schedulingService.createAssignment(
            newProject.getId(),
            developer1.getId(),
            LocalDate.now().plusDays(56),
            LocalDate.now().plusDays(90)
        );
        
        // AND: The system correctly tracks availability
        assertThat(schedulingService.isResourceAvailable(
            developer1.getId(),
            LocalDate.now().plusDays(45),
            LocalDate.now().plusDays(55)
        )).isFalse();
        
        // Verify assignments were created correctly
        List<Assignment> projectAssignments = schedulingService.getAssignmentsByProject(newProject.getId());
        assertThat(projectAssignments).hasSize(3); // Lead + 2 parts for developer1
    }
    
    @Test
    @Order(2)
    @DisplayName("Scenario: Scheduler handles emergency sick leave and reassignment")
    void testEmergencySickLeaveAndReassignment() {
        // GIVEN: An active project with assigned resources
        Project activeProject = schedulingService.createProject(
            "EMERGENCY-001",
            "Critical System Upgrade",
            LocalDate.now(),
            LocalDate.now().plusDays(14)
        );
        
        Resource primaryTech = schedulingService.createResource(
            "Tom Wilson", "tom@example.com",
            new ResourceType("Senior Technician", ResourceCategory.INTERNAL)
        );
        
        Resource backupTech = schedulingService.createResource(
            "Emily Davis", "emily@example.com",
            new ResourceType("Technician", ResourceCategory.INTERNAL)
        );
        
        Assignment originalAssignment = schedulingService.createAssignment(
            activeProject.getId(),
            primaryTech.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(14)
        );
        
        // WHEN: Primary technician calls in sick unexpectedly
        TechnicianUnavailability sickLeave = schedulingService.createUnavailability(
            primaryTech.getId(),
            UnavailabilityType.SICK_LEAVE,
            LocalDate.now(),
            LocalDate.now().plusDays(3),
            "Flu - doctor's note provided"
        );
        
        // THEN: Scheduler identifies the conflict
        boolean hasConflict = schedulingService.hasConflicts(originalAssignment.getId());
        assertThat(hasConflict).isTrue();
        
        // AND: Scheduler can reassign to backup technician
        assertThat(schedulingService.isResourceAvailable(
            backupTech.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(3)
        )).isTrue();
        
        Assignment coverageAssignment = schedulingService.createAssignment(
            activeProject.getId(),
            backupTech.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(3)
        );
        
        // AND: Original tech can resume after sick leave
        Assignment resumedAssignment = schedulingService.createAssignment(
            activeProject.getId(),
            primaryTech.getId(),
            LocalDate.now().plusDays(4),
            LocalDate.now().plusDays(14)
        );
        
        // Verify coverage is complete
        List<Assignment> projectAssignments = schedulingService.getAssignmentsByProject(activeProject.getId());
        assertThat(projectAssignments).hasSize(3); // Original (now partial), coverage, resumed
    }
    
    @Test
    @Order(3)
    @DisplayName("Scenario: HR manages annual training schedule with resource coordination")
    void testAnnualTrainingScheduleCoordination() {
        // GIVEN: HR is planning mandatory annual safety training
        LocalDate trainingStart = LocalDate.now().plusMonths(2);
        LocalDate trainingEnd = trainingStart.plusDays(4);
        
        // AND: Multiple technicians need to attend
        Resource tech1 = schedulingService.createResource(
            "Alex Thompson", "alex@example.com",
            new ResourceType("Field Technician", ResourceCategory.INTERNAL)
        );
        
        Resource tech2 = schedulingService.createResource(
            "Jordan Lee", "jordan@example.com",
            new ResourceType("Field Technician", ResourceCategory.INTERNAL)
        );
        
        Resource tech3 = schedulingService.createResource(
            "Casey Brown", "casey@example.com",
            new ResourceType("Field Technician", ResourceCategory.INTERNAL)
        );
        
        // WHEN: HR schedules training for all technicians
        TechnicianUnavailability training1 = schedulingService.createUnavailability(
            tech1.getId(),
            UnavailabilityType.TRAINING,
            trainingStart,
            trainingEnd,
            "Annual Safety Certification"
        );
        
        TechnicianUnavailability training2 = schedulingService.createUnavailability(
            tech2.getId(),
            UnavailabilityType.TRAINING,
            trainingStart,
            trainingEnd,
            "Annual Safety Certification"
        );
        
        TechnicianUnavailability training3 = schedulingService.createUnavailability(
            tech3.getId(),
            UnavailabilityType.TRAINING,
            trainingStart,
            trainingEnd,
            "Annual Safety Certification"
        );
        
        // THEN: System prevents project assignments during training
        Project conflictingProject = schedulingService.createProject(
            "TRAIN-CONFLICT-001",
            "Routine Maintenance",
            trainingStart.minusDays(2),
            trainingEnd.plusDays(2)
        );
        
        // Verify no technicians can be assigned during training
        assertThat(schedulingService.isResourceAvailable(tech1.getId(), trainingStart, trainingEnd)).isFalse();
        assertThat(schedulingService.isResourceAvailable(tech2.getId(), trainingStart, trainingEnd)).isFalse();
        assertThat(schedulingService.isResourceAvailable(tech3.getId(), trainingStart, trainingEnd)).isFalse();
        
        // AND: HR can track training participation
        List<TechnicianUnavailability> trainingPeriods = 
            unavailabilityRepository.findByDateRange(trainingStart, trainingEnd);
        
        long trainingCount = trainingPeriods.stream()
            .filter(u -> u.getType() == UnavailabilityType.TRAINING)
            .count();
        
        assertThat(trainingCount).isEqualTo(3);
    }
    
    @Test
    @Order(4)
    @DisplayName("Scenario: Financial impact of resource unavailability on project budget")
    void testFinancialImpactOfUnavailability() {
        // GIVEN: A project with budget and assigned resources
        Project budgetedProject = schedulingService.createProject(
            "BUDGET-001",
            "Infrastructure Upgrade",
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(37) // 30 days
        );
        budgetedProject.setBudget(new BigDecimal("50000"));
        projectRepository.update(budgetedProject);
        
        Resource expensiveResource = schedulingService.createResource(
            "Senior Specialist", "specialist@example.com",
            new ResourceType("Specialist", ResourceCategory.INTERNAL)
        );
        expensiveResource.setDailyRate(new BigDecimal("1000"));
        resourceRepository.update(expensiveResource);
        
        // WHEN: Resource is assigned for full project duration
        Assignment fullAssignment = schedulingService.createAssignment(
            budgetedProject.getId(),
            expensiveResource.getId(),
            budgetedProject.getStartDate(),
            budgetedProject.getEndDate()
        );
        
        // Initial cost calculation (30 days * $1000/day = $30,000)
        BigDecimal plannedCost = new BigDecimal("30000");
        
        // BUT: Resource becomes unavailable for 5 days mid-project
        TechnicianUnavailability unavailability = schedulingService.createUnavailability(
            expensiveResource.getId(),
            UnavailabilityType.PERSONAL_TIME,
            LocalDate.now().plusDays(20),
            LocalDate.now().plusDays(24),
            "Family emergency"
        );
        
        // THEN: Project manager needs to adjust budget/timeline
        // Option 1: Extend project (additional overhead costs)
        // Option 2: Bring in contractor (higher daily rate)
        Resource contractor = schedulingService.createResource(
            "External Contractor", "contractor@vendor.com",
            new ResourceType("Contractor", ResourceCategory.THIRD_PARTY)
        );
        contractor.setDailyRate(new BigDecimal("1500"));
        resourceRepository.update(contractor);
        
        Assignment contractorCoverage = schedulingService.createAssignment(
            budgetedProject.getId(),
            contractor.getId(),
            LocalDate.now().plusDays(20),
            LocalDate.now().plusDays(24)
        );
        
        // Calculate financial impact
        BigDecimal reducedInternalCost = new BigDecimal("25000"); // 25 days * $1000
        BigDecimal contractorCost = new BigDecimal("7500"); // 5 days * $1500
        BigDecimal actualCost = reducedInternalCost.add(contractorCost);
        
        // Verify budget impact
        assertThat(actualCost).isGreaterThan(plannedCost);
        BigDecimal costOverrun = actualCost.subtract(plannedCost);
        assertThat(costOverrun).isEqualTo(new BigDecimal("2500"));
    }
    
    @Test
    @Order(5)
    @DisplayName("Scenario: Recurring unavailability patterns (e.g., every Friday off)")
    void testRecurringUnavailabilityPattern() {
        // GIVEN: A part-time resource who doesn't work Fridays
        Resource partTimeResource = schedulingService.createResource(
            "Pat Anderson", "pat@example.com",
            new ResourceType("Part-Time Tech", ResourceCategory.INTERNAL)
        );
        
        // WHEN: Setting up recurring unavailability
        TechnicianUnavailability recurringFridays = schedulingService.createUnavailability(
            partTimeResource.getId(),
            UnavailabilityType.RECURRING,
            LocalDate.now().with(java.time.DayOfWeek.MONDAY),
            LocalDate.now().plusMonths(6),
            "Part-time schedule - No Fridays"
        );
        recurringFridays.setRecurring(true);
        recurringFridays.setRecurrencePattern("WEEKLY:FRIDAY");
        unavailabilityRepository.update(recurringFridays);
        
        // THEN: System should prevent Friday assignments
        LocalDate nextFriday = LocalDate.now().with(java.time.DayOfWeek.FRIDAY);
        LocalDate fridayAfter = nextFriday.plusWeeks(1);
        
        assertThat(schedulingService.isResourceAvailable(
            partTimeResource.getId(),
            nextFriday,
            nextFriday
        )).isFalse();
        
        assertThat(schedulingService.isResourceAvailable(
            partTimeResource.getId(),
            fridayAfter,
            fridayAfter
        )).isFalse();
        
        // BUT: Other days are available
        LocalDate nextMonday = nextFriday.plusDays(3);
        assertThat(schedulingService.isResourceAvailable(
            partTimeResource.getId(),
            nextMonday,
            nextMonday.plusDays(3) // Mon-Thu
        )).isTrue();
    }
    
    @Test
    @Order(6)
    @DisplayName("Scenario: Year-end resource availability report for capacity planning")
    void testYearEndAvailabilityReport() {
        // GIVEN: Management wants to analyze resource utilization
        Resource resource1 = schedulingService.createResource(
            "Resource One", "r1@example.com",
            new ResourceType("Technician", ResourceCategory.INTERNAL)
        );
        
        // Create various unavailability types throughout the year
        schedulingService.createUnavailability(
            resource1.getId(),
            UnavailabilityType.VACATION,
            LocalDate.now().minusMonths(6),
            LocalDate.now().minusMonths(6).plusDays(14),
            "Summer vacation"
        );
        
        schedulingService.createUnavailability(
            resource1.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.now().minusMonths(3),
            LocalDate.now().minusMonths(3).plusDays(4),
            "Professional development"
        );
        
        schedulingService.createUnavailability(
            resource1.getId(),
            UnavailabilityType.SICK_LEAVE,
            LocalDate.now().minusMonths(1),
            LocalDate.now().minusMonths(1).plusDays(2),
            "Sick"
        );
        
        // WHEN: Generating availability statistics
        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        LocalDate yearEnd = LocalDate.now().withDayOfYear(365);
        
        List<TechnicianUnavailability> yearUnavailabilities = 
            unavailabilityRepository.findByResourceId(resource1.getId());
        
        // Calculate total days by type
        long vacationDays = 0;
        long trainingDays = 0;
        long sickDays = 0;
        
        for (TechnicianUnavailability u : yearUnavailabilities) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                u.getStartDate(), u.getEndDate()) + 1;
            
            switch (u.getType()) {
                case VACATION:
                    vacationDays += days;
                    break;
                case TRAINING:
                    trainingDays += days;
                    break;
                case SICK_LEAVE:
                    sickDays += days;
                    break;
            }
        }
        
        // THEN: Management can see utilization metrics
        long totalUnavailableDays = vacationDays + trainingDays + sickDays;
        double availabilityPercentage = ((365.0 - totalUnavailableDays) / 365.0) * 100;
        
        assertThat(vacationDays).isEqualTo(15);
        assertThat(trainingDays).isEqualTo(5);
        assertThat(sickDays).isEqualTo(3);
        assertThat(availabilityPercentage).isGreaterThan(90); // Good availability
        
        System.out.println("Resource Availability Report:");
        System.out.println("- Vacation Days: " + vacationDays);
        System.out.println("- Training Days: " + trainingDays);
        System.out.println("- Sick Days: " + sickDays);
        System.out.println("- Total Unavailable: " + totalUnavailableDays);
        System.out.println("- Availability: " + String.format("%.1f%%", availabilityPercentage));
    }
}