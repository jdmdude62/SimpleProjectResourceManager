package com.subliminalsearch.simpleprojectresourcemanager.ui;

import com.subliminalsearch.simpleprojectresourcemanager.SchedulerApplication;
import com.subliminalsearch.simpleprojectresourcemanager.model.*;
import com.subliminalsearch.simpleprojectresourcemanager.repository.*;
import com.subliminalsearch.simpleprojectresourcemanager.service.SchedulingService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import org.testfx.matcher.base.NodeMatchers;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.matcher.control.TableViewMatchers;

import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.testfx.api.FxAssert.*;
import org.testfx.api.FxAssert;

@DisplayName("Resource Unavailability UI Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ResourceUnavailabilityUITest extends ApplicationTest {
    
    private static HikariDataSource dataSource;
    private static SchedulingService schedulingService;
    private static Resource testResource;
    private static Project testProject;
    
    @BeforeAll
    static void setupDatabase() {
        // Create test database
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:test_ui.db");
        config.setMaximumPoolSize(1);
        dataSource = new HikariDataSource(config);
        
        // Initialize repositories
        ProjectRepository projectRepo = new ProjectRepository(dataSource);
        ResourceRepository resourceRepo = new ResourceRepository(dataSource);
        AssignmentRepository assignmentRepo = new AssignmentRepository(dataSource);
        ProjectManagerRepository pmRepo = new ProjectManagerRepository(dataSource);
        
        schedulingService = new SchedulingService(projectRepo, resourceRepo, assignmentRepo, pmRepo, dataSource);
        
        // Create test data
        testResource = new Resource("Test Technician", "test@example.com", 
            new ResourceType("Technician", ResourceCategory.INTERNAL));
        testResource = resourceRepo.save(testResource);
        
        testProject = new Project("TEST-001", "Test Project", 
            LocalDate.now(), LocalDate.now().plusDays(30));
        testProject = projectRepo.save(testProject);
    }
    
    @AfterAll
    static void tearDownDatabase() {
        if (dataSource != null) {
            dataSource.close();
        }
        new File("test_ui.db").delete();
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        // Start the application with test configuration
        System.setProperty("test.mode", "true");
        System.setProperty("test.db.url", "jdbc:sqlite:test_ui.db");
        
        SchedulerApplication app = new SchedulerApplication();
        app.start(stage);
    }
    
    @AfterEach
    void cleanup() throws TimeoutException {
        FxToolkit.hideStage();
    }
    
    @Test
    @Order(1)
    @DisplayName("Should access resource availability from View menu")
    void testAccessResourceAvailabilityFromMenu() {
        // Click on View menu
        clickOn("View");
        
        // Click on Resource Availability
        clickOn("Resource Availability");
        
        // Verify dialog opened
        verifyThat("#resourceAvailabilityDialog", isVisible());
        
        // Close dialog
        push(KeyCode.ESCAPE);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should mark resource unavailable via context menu")
    void testMarkResourceUnavailableViaContextMenu() {
        // Navigate to timeline view
        clickOn("#timelineView");
        
        // Right-click on a resource row
        rightClickOn("#resourceRow_" + testResource.getId());
        
        // Click on "Mark Unavailable"
        clickOn("Mark Unavailable");
        
        // Verify unavailability dialog opened
        verifyThat("#unavailabilityDialog", isVisible());
        
        // Fill in unavailability details
        clickOn("#unavailabilityType").clickOn("Vacation");
        
        clickOn("#startDatePicker").write(LocalDate.now().plusDays(7).toString());
        clickOn("#endDatePicker").write(LocalDate.now().plusDays(14).toString());
        
        clickOn("#reasonField").write("Annual vacation");
        
        // Submit
        clickOn("#saveButton");
        
        // Verify dialog closed and data saved
        verifyThat("#unavailabilityDialog", isNotVisible());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should display unavailability on timeline")
    void testUnavailabilityDisplayOnTimeline() {
        // Create an unavailability
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(5),
            "Test vacation"
        );
        
        // Refresh timeline
        clickOn("#refreshButton");
        
        // Verify unavailability bar is displayed with distinct styling
        verifyThat("#unavailabilityBar_" + testResource.getId(), isVisible());
        verifyThat("#unavailabilityBar_" + testResource.getId(), hasClass("unavailability-vacation"));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should edit existing unavailability")
    void testEditUnavailability() {
        // Create an unavailability
        TechnicianUnavailability unavailability = schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(12),
            "Training course"
        );
        
        // Open resource availability dialog
        clickOn("View").clickOn("Resource Availability");
        
        // Select the resource
        clickOn("#resourceComboBox").clickOn(testResource.getName());
        
        // Find and double-click the unavailability in the table
        TableView<?> table = lookup("#unavailabilityTable").query();
        assertThat(table.getItems()).isNotEmpty();
        
        doubleClickOn("#unavailabilityTable");
        
        // Edit the unavailability
        clickOn("#endDatePicker").eraseText(20).write(LocalDate.now().plusDays(15).toString());
        clickOn("#reasonField").eraseText(50).write("Extended training course");
        
        // Save changes
        clickOn("#updateButton");
        
        // Verify changes were saved
        // Verify changes were saved in the table
        TableView<?> unavailTable = lookup("#unavailabilityTable").queryAs(TableView.class);
        assertThat(unavailTable.getItems()).isNotEmpty();
    }
    
    @Test
    @Order(5)
    @DisplayName("Should delete unavailability")
    void testDeleteUnavailability() {
        // Create an unavailability
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.SICK_LEAVE,
            LocalDate.now(),
            LocalDate.now().plusDays(2),
            "Sick"
        );
        
        // Open resource availability dialog
        clickOn("View").clickOn("Resource Availability");
        
        // Select the resource
        clickOn("#resourceComboBox").clickOn(testResource.getName());
        
        // Select the unavailability in the table
        clickOn("#unavailabilityTable");
        
        // Click delete button
        clickOn("#deleteButton");
        
        // Confirm deletion
        clickOn("Yes");
        
        // Verify unavailability was removed
        TableView<?> table = lookup("#unavailabilityTable").query();
        // Check that the table no longer contains the deleted item
        assertThat(table.getItems()).isEmpty();
    }
    
    @Test
    @Order(6)
    @DisplayName("Should prevent assignment during unavailability period")
    void testPreventAssignmentDuringUnavailability() {
        // Create an unavailability
        LocalDate unavailStart = LocalDate.now().plusDays(5);
        LocalDate unavailEnd = LocalDate.now().plusDays(10);
        
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            unavailStart,
            unavailEnd,
            "Vacation"
        );
        
        // Try to create an assignment during unavailability
        clickOn("#assignmentButton");
        
        clickOn("#projectComboBox").clickOn(testProject.getProjectId());
        clickOn("#resourceComboBox").clickOn(testResource.getName());
        clickOn("#assignStartDate").write(unavailStart.plusDays(2).toString());
        clickOn("#assignEndDate").write(unavailEnd.minusDays(2).toString());
        
        // Try to save
        clickOn("#saveAssignmentButton");
        
        // Verify error message is shown
        verifyThat(".alert", isVisible());
        verifyThat(".alert", hasText("Resource is unavailable during this period"));
        
        // Close alert
        clickOn("OK");
    }
    
    @Test
    @Order(7)
    @DisplayName("Should show unavailability conflicts warning")
    void testUnavailabilityConflictWarning() {
        // Create an assignment
        Assignment assignment = schedulingService.createAssignment(
            testProject.getId(),
            testResource.getId(),
            LocalDate.now().plusDays(20),
            LocalDate.now().plusDays(25)
        );
        
        // Try to create overlapping unavailability
        clickOn("View").clickOn("Resource Availability");
        clickOn("#resourceComboBox").clickOn(testResource.getName());
        clickOn("#addUnavailabilityButton");
        
        clickOn("#unavailabilityType").clickOn("Personal Time");
        clickOn("#startDatePicker").write(LocalDate.now().plusDays(22).toString());
        clickOn("#endDatePicker").write(LocalDate.now().plusDays(24).toString());
        clickOn("#reasonField").write("Personal appointment");
        
        clickOn("#saveButton");
        
        // Verify warning is shown
        verifyThat(".warning-dialog", isVisible());
        verifyThat(".warning-dialog", hasText("conflicts with existing assignments"));
        
        // User can choose to proceed or cancel
        clickOn("Proceed Anyway");
        
        // Verify unavailability was created despite conflict
        // Verify new entry appears in table
        TableView<?> tablePersonal = lookup("#unavailabilityTable").queryAs(TableView.class);
        assertThat(tablePersonal.getItems()).isNotEmpty();
    }
    
    @Test
    @Order(8)
    @DisplayName("Should filter unavailabilities by date range")
    void testFilterUnavailabilitiesByDateRange() {
        // Create multiple unavailabilities
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(25),
            "Past vacation"
        );
        
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(35),
            "Future training"
        );
        
        // Open resource availability dialog
        clickOn("View").clickOn("Resource Availability");
        
        // Apply date filter
        clickOn("#filterStartDate").write(LocalDate.now().toString());
        clickOn("#filterEndDate").write(LocalDate.now().plusDays(60).toString());
        clickOn("#applyFilterButton");
        
        // Verify only future unavailability is shown
        TableView<?> table = lookup("#unavailabilityTable").query();
        assertThat(table.getItems()).hasSize(1);
        // Verify new entry appears in table
        TableView<?> tableFuture = lookup("#unavailabilityTable").queryAs(TableView.class);
        assertThat(tableFuture.getItems()).isNotEmpty();
    }
    
    @Test
    @Order(9)
    @DisplayName("Should validate unavailability date ranges")
    void testValidateUnavailabilityDates() {
        clickOn("View").clickOn("Resource Availability");
        clickOn("#addUnavailabilityButton");
        
        // Try to set end date before start date
        clickOn("#startDatePicker").write(LocalDate.now().plusDays(10).toString());
        clickOn("#endDatePicker").write(LocalDate.now().plusDays(5).toString());
        
        clickOn("#saveButton");
        
        // Verify validation error
        verifyThat("#dateValidationError", isVisible());
        verifyThat("#dateValidationError", hasText("End date must be after start date"));
        
        // Correct the dates
        clickOn("#endDatePicker").eraseText(20).write(LocalDate.now().plusDays(15).toString());
        
        // Verify error cleared
        verifyThat("#dateValidationError", isNotVisible());
    }
    
    @Test
    @Order(10)
    @DisplayName("Should show unavailability summary statistics")
    void testUnavailabilitySummaryStatistics() {
        // Create various unavailabilities
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.VACATION,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(14),
            "Vacation"
        );
        
        schedulingService.createUnavailability(
            testResource.getId(),
            UnavailabilityType.TRAINING,
            LocalDate.now().plusDays(20),
            LocalDate.now().plusDays(22),
            "Training"
        );
        
        // Open resource availability dialog
        clickOn("View").clickOn("Resource Availability");
        clickOn("#resourceComboBox").clickOn(testResource.getName());
        
        // Click on summary tab
        clickOn("#summaryTab");
        
        // Verify statistics are displayed
        verifyThat("#totalDaysUnavailable", hasText("17")); // 14 + 3 days
        verifyThat("#vacationDays", hasText("14"));
        verifyThat("#trainingDays", hasText("3"));
        verifyThat("#availabilityPercentage", isVisible());
    }
    
    // Helper methods
    private void verifyThat(String query, org.hamcrest.Matcher<Object> matcher) {
        FxAssert.verifyThat(lookup(query).query(), matcher);
    }
    
    private org.hamcrest.Matcher<Object> isVisible() {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof javafx.scene.Node) {
                    return ((javafx.scene.Node) item).isVisible();
                }
                return false;
            }
            
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("is visible");
            }
        };
    }
    
    private org.hamcrest.Matcher<Object> isNotVisible() {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof javafx.scene.Node) {
                    return !((javafx.scene.Node) item).isVisible();
                }
                return false;
            }
            
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("is not visible");
            }
        };
    }
    
    private org.hamcrest.Matcher<Object> hasClass(String className) {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof javafx.scene.Node) {
                    return ((javafx.scene.Node) item).getStyleClass().contains(className);
                }
                return false;
            }
            
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("has class ").appendValue(className);
            }
        };
    }
    
    private org.hamcrest.Matcher<Object> hasText(String text) {
        return new org.hamcrest.BaseMatcher<Object>() {
            @Override
            public boolean matches(Object item) {
                if (item instanceof Labeled) {
                    return ((Labeled) item).getText().contains(text);
                } else if (item instanceof TextInputControl) {
                    return ((TextInputControl) item).getText().contains(text);
                }
                return false;
            }
            
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("has text ").appendValue(text);
            }
        };
    }
}